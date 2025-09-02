package com.example.emr_server.service.urpl;

import java.util.List;

public class UrplPageResponse {
    private List<UrplProduct> content;
    private int totalPages;
    private long totalElements;
    private int number;
    private int size;
    private boolean last;

    public List<UrplProduct> getContent() { return content; }
    public void setContent(List<UrplProduct> content) { this.content = content; }
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    public long getTotalElements() { return totalElements; }
    public void setTotalElements(long totalElements) { this.totalElements = totalElements; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public int getSize() { return size; }
    public void setSize(int size) { this.size = size; }
    public boolean isLast() { return last; }
    public void setLast(boolean last) { this.last = last; }
}

