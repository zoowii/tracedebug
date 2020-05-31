package classinjector;

import org.aspectj.apache.bcel.Constants;
import org.aspectj.apache.bcel.Repository;
import org.aspectj.apache.bcel.classfile.*;
import org.aspectj.apache.bcel.classfile.annotation.AnnotationGen;
import org.aspectj.apache.bcel.generic.*;
import org.objectweb.asm.Opcodes;

import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ClassInjector {

    private void injectMethod(ClassGen cg, Class<?> sourceCls, MethodGen methodGen,
                              org.aspectj.apache.bcel.classfile.Method method,
                              org.aspectj.apache.bcel.classfile.Method originMethodCopy,
                              InstructionList instructionList,
                              InstructionFactory factory) throws Exception {
        if (instructionList.isEmpty()) {
            return;
        }

        InstructionHandle oldBodyStart = instructionList.getStart();

        // 从当前线程中判断当前线程的traceId是否需要加dump(根据采样策略判断)，如果不需要加调试，直接调用super.method(...)
        InstructionBranch enableCheckJmpToStartInst = injectEnableCheck(cg, sourceCls, methodGen, method, originMethodCopy, instructionList, factory, oldBodyStart);

        // 开头加入spanStart方法调用产生新的spanId
        InjectSpanStartResult injectSpanResult = injectSpanStart(methodGen, method, instructionList, factory, oldBodyStart);
        int spanIdVarIndex = injectSpanResult.spanIdVarGen.getIndex();

        if (enableCheckJmpToStartInst != null) {
            enableCheckJmpToStartInst.setTarget(injectSpanResult.firstAdded);
        }

        // 开头也要加入dump指令
        injectDumpLocalVars(method, instructionList, factory, oldBodyStart, spanIdVarIndex, true);

        // 结尾加入dump
        injectDumpLocalVars(method, instructionList, factory, instructionList.getEnd(), spanIdVarIndex, false); // TODO： 因为store指令后有导出，所以可以考虑结尾和return语句后不dump

        InstructionHandle newLastReturn = instructionList.getEnd();

        // 对除最后一条return语句外的其他return语句，如果不是带返回值的，跳转到结尾的return指令前，否则也插入dump指令
        Iterator<?> it = instructionList.iterator();
        while (it.hasNext()) {
            InstructionHandle ih = (InstructionHandle) it.next();
            // TODO: 暂时都注入dump指令，而不是跳转
            if (!ih.getInstruction().isReturnInstruction()) {
                continue;
            }
            if (ih.getPosition() < 0) {
                // 是注入的return语句，不需要修改
                continue;
            }
            if (ih == newLastReturn) {
                continue;
            }
            injectDumpLocalVars(method, instructionList, factory, ih, spanIdVarIndex, false);
        }

        // 在变量修改的指令(除了注入的指令)后也dump单个指令
        LocalVariableTable localVariableTable = methodGen.getLocalVariableTable(methodGen.getConstantPool());
        Iterator<?> it2 = instructionList.iterator();
        while (it2.hasNext()) {
            InstructionHandle ih = (InstructionHandle) it2.next();
            if (ih.getPosition() < 0) {
                continue; // 注入的指令，不需要处理
            }
            if (ih.getNext() == null) {
                continue;
            }
            Instruction ihi = ih.getInstruction();
            if (ihi instanceof InstructionLV && ihi.toString().contains("store")) {
                InstructionLV storeInst = (InstructionLV) ihi;
                LocalVariable localVar = localVariableTable.getLocalVariable(storeInst.getIndex());
                injectDumpLocalVar(method, instructionList, factory, ih.getNext(), spanIdVarIndex, localVar, false);
            }
        }

        instructionList.setPositions(true);
    }

    private final String uniqueSpanIdVarName = "spanId$";

    private static class InjectSpanStartResult {
        private LocalVariableGen spanIdVarGen;
        private InstructionHandle firstAdded;
        private InstructionHandle lastAddedIh;

        public InjectSpanStartResult(LocalVariableGen spanIdVarGen, InstructionHandle firstAdded, InstructionHandle lastAddedIh) {
            this.spanIdVarGen = spanIdVarGen;
            this.firstAdded = firstAdded;
            this.lastAddedIh = lastAddedIh;
        }
    }

    /**
     * 给方法指令注入spanStart调用的指令产生spanId到一个新的局部变量
     */
    private InjectSpanStartResult injectSpanStart(MethodGen methodGen, org.aspectj.apache.bcel.classfile.Method method,
                                                  InstructionList instructionList, InstructionFactory factory, InstructionHandle before) {
        // 产生一个不会重复的局部变量保存spanId
        LocalVariableGen spanIdVarGen = methodGen.addLocalVariable(uniqueSpanIdVarName, Type.STRING, before, instructionList.getEnd());
        InstructionHandle firstAdded = instructionList.insert(before, factory.createInvoke(StackDumper.class.getName(),
                "spanStart", Type.STRING, new Type[0], Constants.INVOKESTATIC));
        spanIdVarGen.setStart(instructionList.getStart());
        InstructionHandle lastAddedIh = instructionList.insert(before, factory.createStore(Type.STRING, spanIdVarGen.getIndex()));
        return new InjectSpanStartResult(spanIdVarGen, firstAdded, lastAddedIh);
    }

    private void injectDumpLocalVar(org.aspectj.apache.bcel.classfile.Method method, InstructionList instructionList,
                                    InstructionFactory factory, InstructionHandle before, int spanIdVarIndex, LocalVariable localVariable, boolean isMethodStart) {
        LocalVariableTable localVariableTable = method.getLocalVariableTable();
        Type localVarType = Type.getType(localVariable.getSignature());
        if (localVariable.getName().equals(uniqueSpanIdVarName)) {
            return;
        }
        if (localVariable.getName().equals(enableTraceVarName)) {
            return;
        }
        List<Instruction> loadVarInsts = new ArrayList<Instruction>();

        // 如果是局部变量并且当前位置在局部变量定义生命周期之外，也不注入
        if(localVariable.getStartPC() > before.getPosition()) {
            return;
        }

        if (!(localVarType instanceof ObjectType)) {
            // TODO: 如果是基本类型，需要把基本类型转换成Object类型然后dump
            if(localVarType.getSignature().equals(Type.INT.getSignature())) {
                // 是int类型时，调用 Integer.valueOf(value)
                loadVarInsts.add(factory.createLoad(Type.INT, localVariable.getIndex()));
                loadVarInsts.add(factory.createInvoke("java.lang.Integer", "valueOf",
                        Type.INTEGER, new Type[]{Type.INT}, Constants.INVOKESTATIC));
            }
            // TODO: 其他基本类型的注入
        } else {
            // 加载对象类型的局部变量
            loadVarInsts.add(factory.createLoad(localVarType, localVariable.getIndex()));
        }
        if(loadVarInsts.isEmpty()) {
            return;
        }
        instructionList.insert(before, factory.createLoad(Type.STRING, spanIdVarIndex));
        instructionList.insert(before, factory.createConstant(localVariable.getName()));
        for(Instruction inst : loadVarInsts) {
            instructionList.insert(before, inst);
        }
        int lineNumber = -1; // before所在行号
        lineNumber = method.getLineNumberTable().getSourceLine(before.getPosition());
        method.getConstantPool().addInteger(lineNumber);
//            System.out.println("line="+lineNumber);
        instructionList.insert(before, factory.createConstant(lineNumber));
        instructionList.insert(before, factory.createInvoke(StackDumper.class.getName(),
                "dump", Type.VOID, new Type[]{Type.STRING, Type.STRING, Type.OBJECT, Type.INT},
                Constants.INVOKESTATIC));
    }

    /**
     * 给方法指令注入dump局部变量内容的指令
     */
    private void injectDumpLocalVars(org.aspectj.apache.bcel.classfile.Method method, InstructionList instructionList,
                                     InstructionFactory factory, InstructionHandle before, int spanIdVarIndex, boolean isMethodStart) {
        LocalVariableTable localVariableTable = method.getLocalVariableTable();
        // TODO: 改成只导出函数参数
        // add dump this code
        for (LocalVariable localVariable : localVariableTable.getLocalVariableTable()) {
            injectDumpLocalVar(method, instructionList, factory, before, spanIdVarIndex, localVariable, isMethodStart);
        }
    }

    String enableTraceVarName = "enableTrace$";

    /**
     * 插入判断是否开启debug trace的指令
     */
    private InstructionBranch injectEnableCheck(ClassGen cg, Class<?> sourceClass, MethodGen methodGen,
                                                org.aspectj.apache.bcel.classfile.Method method,
                                                org.aspectj.apache.bcel.classfile.Method originMethodCopy,
                                                InstructionList instructionList,
                                                InstructionFactory factory, InstructionHandle before) {
//        String enableTraceVarName = "enableTrace$"; // TODO: 定义名称不冲突的局部变量
        LocalVariableGen enableTraceVarGen = methodGen.addLocalVariable(enableTraceVarName, Type.BOOLEAN, before, instructionList.getEnd());
        instructionList.insert(before, factory.createInvoke(StackDumper.class.getName(), "isDebugTraceEnabledTrace",
                Type.BOOLEAN, new Type[0], Constants.INVOKESTATIC));
        instructionList.insert(before, factory.createStore(Type.BOOLEAN, enableTraceVarGen.getIndex()));
        instructionList.insert(before, factory.createLoad(Type.BOOLEAN, enableTraceVarGen.getIndex()));
        InstructionBranch jmpToNewStartIns = factory.createBranchInstruction((short) Opcodes.IFNE, before); // 调用后要修改为跳转到spanStart语句开头
        instructionList.insert(before, jmpToNewStartIns);
        // 调用父类的本方法. 区分有返回值和没返回值的情况. 如果有参数，参数要原样传入
        // 插入参数
        LocalVariableTable localVariableTable = methodGen.getLocalVariableTable(methodGen.getConstantPool());
        for (int i = 0; i < method.getArgumentTypes().length + 1; i++) {
            instructionList.insert(before, factory.createLoad(
                    Type.getType(localVariableTable.getLocalVariable(i).getSignature()),
                    localVariableTable.getLocalVariable(i).getIndex()));
        }
        instructionList.insert(before, factory.createInvoke(cg.getClassName(), originMethodCopy.getName(),
                method.getReturnType(), method.getArgumentTypes(), Constants.INVOKEVIRTUAL));
        if (method.getReturnType().equals(Type.VOID)) {
            // return语句
            instructionList.insert(before, factory.createReturn(Type.VOID));
        } else {
            // TODO: 根据返回类型加入合适的return类语句. 目前把method最后一条指令（一般是return类指令）抄过来
            instructionList.insert(before, instructionList.getEnd().getInstruction());
        }
        return jmpToNewStartIns;
    }

    public <T> Class<? extends T> addDumpToMethods(Class<? extends T> sourceClass, List<Method> methods, String newClsName) throws Exception {
        JavaClass cls = Repository.lookupClass(sourceClass.getName());
        // 继承原类
        ConstantPool cpool = cls.getConstantPool();
        String packageName = sourceClass.getPackage().getName();
        String sourceOnlyFilename = cls.getSourceFileName();
        String targetFilename;
        if(packageName.isEmpty()) {
            targetFilename = sourceOnlyFilename;
        } else {
            targetFilename = packageName.replaceAll("[.]", "/") + "/" + sourceOnlyFilename;
        }
        ClassGen cg = new ClassGen(newClsName, sourceClass.getName(), targetFilename,
                cls.getModifiers(), null, cpool);
        // 原类的注解也要加上
        for (AnnotationGen anno : cls.getAnnotations()) {
            cg.addAnnotation(anno);
        }

        for (Method sourceMethod : methods) {
            // 只处理public的虚方法，并且不是静态方法
            if ((sourceMethod.getModifiers() & Opcodes.ACC_PUBLIC) == 0) {
                continue;
            }
            if ((sourceMethod.getModifiers() & Opcodes.ACC_STATIC) != 0) {
                continue;
            }
            org.aspectj.apache.bcel.classfile.Method method = cls.getMethod(sourceMethod);
            Code methodCode = method.getCode();
            // instructionList 要取method方法现有字节码
            InstructionList instructionList = new InstructionList(methodCode.getCode());

            // 从原方法获取信息
            MethodGen methodGen = new MethodGen(method, cls.getClassName(), cpool);

            InstructionFactory factory = new InstructionFactory(cg);

            // 复制一个原方法内容但是名称不同的方法
            String clonedMethodName = sourceMethod.getName() + "$origin"; // TODO: 需要是不冲突的新方法名
            MethodGen originMethodGen = new MethodGen(method, cls.getClassName(), cpool);
            originMethodGen.setName(clonedMethodName);
            originMethodGen.removeAnnotations(); // 复制的方法不需要注解
            originMethodGen.setModifiers(Opcodes.ACC_PRIVATE); // 复制的方法改成private
            originMethodGen.setInstructionList(new InstructionList(methodCode.getCode()));
            org.aspectj.apache.bcel.classfile.Method originMethodCopy = originMethodGen.getMethod();
            cg.addMethod(originMethodCopy);

            injectMethod(cg, sourceClass, methodGen, method, originMethodCopy, instructionList, factory);

            instructionList.setPositions(true);

//            System.out.println("updated il: " + instructionList);

            // methodGen要从method中继承内容
            methodGen.setInstructionList(instructionList);

            methodGen.setMaxStack();
            methodGen.setMaxLocals();
            methodGen.setModifiers(method.getModifiers());

//        TODO: infer and set StackMap


            org.aspectj.apache.bcel.classfile.Method modifiedMethod = methodGen.getMethod();
            instructionList.dispose();


//            System.out.println("modified method code " + modifiedMethod.getCode().getCodeString());

            cg.addMethod(modifiedMethod);
        }

        // add constructor
        cg.addEmptyConstructor(Opcodes.ACC_PUBLIC);

        cls = cg.getJavaClass();

//        Repository.addClass(cls);
        cls.dump("target/classes/" + cls.getClassName().replace(".", "/") + ".class");

        return (Class<? extends T>) Class.forName(cls.getClassName());
    }
}
