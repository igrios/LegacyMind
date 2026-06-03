package com.ignacio.legacyanalyzer.domain.services;



  public class NodeTypeResolver {

    public String resolve(String nodeId) {

        if (nodeId.contains("@")) {

            return "DBLINK";
        }

        if (nodeId.contains(".")) {

            return "PROCEDURE";
        }

        return "TABLE";
    }
}


