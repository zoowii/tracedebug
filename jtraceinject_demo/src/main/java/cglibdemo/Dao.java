package cglibdemo;

@DemoAnno
public class Dao {
    private Dao instance = null; // = new Dao();

    @DemoAnno
    public void update(Integer a) {
        int b = a * 2;
        if (a > 300) {
            System.out.println("a>300 and return");
            return;
        }
        for(int i=0;i<5;i++) {
            a = a + 2;
        }
        hi();
        System.out.println(Integer.valueOf(b));
//        Integer c = b; // TODO: 这种情况会出BUG
        System.out.println("PeopleDao.update() " + b);
    }

    @DemoAnno
    public void hi() {
        System.out.println("hi-Dao");
    }

    @DemoAnno
    public int sum(int a, int b) {
        return a + b;
    }

    public static void main(String[] args) {
        new Dao().update(111);
    }
}
