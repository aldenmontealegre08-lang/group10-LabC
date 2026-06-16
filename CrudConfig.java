package com.pos;
public class CrudConfig { final String title,table,idColumn,selectSql,searchColumn; final String[] fields; public CrudConfig(String title,String table,String id,String[] fields,String select,String search){this.title=title;this.table=table;this.idColumn=id;this.fields=fields;this.selectSql=select;this.searchColumn=search;} }
