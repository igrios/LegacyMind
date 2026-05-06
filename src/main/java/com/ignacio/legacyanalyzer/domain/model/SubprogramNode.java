package com.ignacio.legacyanalyzer.domain.model;

import java.util.ArrayList;
import java.util.List;

public class SubprogramNode {

    private String name;

    private String qualifiedName;

    private String type;

    private String body;

    private List<String> reads = new ArrayList<>();

    private List<String> writes = new ArrayList<>();

    private List<String> calls = new ArrayList<>();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getReads() {
        return reads;
    }

    public void setReads(List<String> reads) {
        this.reads = reads;
    }

    public List<String> getWrites() {
        return writes;
    }

    public void setWrites(List<String> writes) {
        this.writes = writes;
    }

    public List<String> getCalls() {
        return calls;
    }

    public void setCalls(List<String> calls) {
        this.calls = calls;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    public void setQualifiedName(String qualifiedName) {
        this.qualifiedName = qualifiedName;
    }
}
