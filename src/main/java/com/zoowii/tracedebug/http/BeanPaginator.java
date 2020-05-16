package com.zoowii.tracedebug.http;


import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.util.StringUtils;

import java.io.Serializable;

@Getter
@Setter
@ToString
public class BeanPaginator implements Pageable, Serializable {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int DEFAULT_MAX_PAGE_SIZE = 1000;
    private int page;
    private int pageSize = DEFAULT_PAGE_SIZE;
    private int maxPageSize = DEFAULT_MAX_PAGE_SIZE;
    private Sort sort;
    private String sortBy;

    public BeanPaginator() {
    }

    public BeanPaginator(int page, int pageSize, int maxPageSize, Sort sort, String sortBy) {
        this.page = page;
        this.pageSize = pageSize;
        this.maxPageSize = maxPageSize;
        this.sort = sort;
        this.sortBy = sortBy;
    }

    @Override
    public int getPageSize() {
        if(maxPageSize>0 && pageSize > maxPageSize) {
            this.pageSize = maxPageSize;
        }
        if(this.pageSize<=0) {
            this.pageSize = 1;
        }
        return this.pageSize;
    }

    public int getPage() {
        if(page<=0) {
            page = 1;
        }
        return page;
    }

    public long getOffset() {
        return (getPage()-1) * getPageSize();
    }

    public int getLimit() {
        return getPageSize();
    }

    @Override
    public int getPageNumber() {
        return getPage()-1;
    }

    @Override
    public Pageable next() {
        return new BeanPaginator(getPage()+1, getPageSize(), getMaxPageSize(), getSort(), getSortBy());
    }

    public BeanPaginator previous() {
        return hasPrevious() ? new BeanPaginator(getPage()-1, getPageSize(), getMaxPageSize(), getSort(), getSortBy()) : this;
    }


    @Override
    public Pageable previousOrFirst() {
        return hasPrevious() ? previous() : first();
    }

    @Override
    public Pageable first() {
        return new BeanPaginator(1, getPageSize(), getMaxPageSize(), getSort(), getSortBy());
    }

    @Override
    public boolean hasPrevious() {
        return getPage() > 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (!(o instanceof BeanPaginator)) return false;

        BeanPaginator that = (BeanPaginator) o;

        return new EqualsBuilder()
                .append(page, that.page)
                .append(pageSize, that.pageSize)
                .append(maxPageSize, that.maxPageSize)
                .append(sort, that.sort)
                .append(sortBy, that.sortBy)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(page)
                .append(pageSize)
                .append(maxPageSize)
                .append(sort)
                .append(sortBy)
                .toHashCode();
    }

    @Override
    public Sort getSort() {
        if(sort == null) {
            if(!StringUtils.isEmpty(sortBy)) {
                if(sortBy.startsWith("+")) {
                    return Sort.by(Sort.Direction.ASC, sortBy.substring(1));
                } else if(sortBy.startsWith("-")) {
                    return Sort.by(Sort.Direction.DESC, sortBy.substring(1));
                } else {
                    return Sort.by(Sort.Direction.ASC, sortBy);
                }
            }
            return Sort.by(Sort.Direction.ASC, "id");
        } else {
            return sort;
        }
    }
}