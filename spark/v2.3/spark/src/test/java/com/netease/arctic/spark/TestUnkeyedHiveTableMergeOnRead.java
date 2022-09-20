package com.netease.arctic.spark;

import com.netease.arctic.hive.table.HiveLocationKind;
import com.netease.arctic.table.BaseLocationKind;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.UnkeyedTable;
import org.apache.hadoop.hive.metastore.api.Partition;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.thrift.TException;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class TestUnkeyedHiveTableMergeOnRead extends SparkTestBase {
  private final String database = "db";
  private final String table = "testa";
  private UnkeyedTable unkeyedTable;
  private final TableIdentifier identifier = TableIdentifier.of(catalogName, database, table);

  @Before
  public void before() {
    sql("create database if not exists {0}", database);
  }

  @After
  public void after() {
    sql("drop table {0}.{1}", database, table);
    sql("drop database if exists " + database);
  }



  @Test
  public void testMergeOnReadUnkeyedPartiton() throws IOException, TException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string \n" +
        ") using arctic \n" +
        " partitioned by ( dt ) \n", database, table);
    unkeyedTable = loadTable(identifier).asUnkeyedTable();
    writeHive(unkeyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 1, "aaa", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 2, "bbb", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 3, "ccc", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 4, "ddd", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 5, "eee", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 6, "fff", "2021-1-2")
    ));
    writeHive(unkeyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 7, "aaa_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 8, "bbb_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 9, "ccc_hive", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 10, "ddd_hive", "2021-1-2")
    ));
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(10, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

    List<Partition> partitions = hms.getClient().listPartitions(
        database,
        table,
        (short) -1);
    Assert.assertEquals(2, partitions.size());
    //disable arctic
    sql("set spark.arctic.sql.delegate.enable = false");
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
    sql("set spark.arctic.sql.delegate.enable = true");
  }

  @Test
  public void testMergeOnReadUnkeyedUnpartition() throws IOException {
    sql("create table {0}.{1} ( \n" +
        " id int , \n" +
        " data string , \n " +
        " dt string \n" +
        ") using arctic \n", database, table);
    unkeyedTable = loadTable(identifier).asUnkeyedTable();
    writeHive(unkeyedTable, BaseLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 1, "aaa", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 2, "bbb", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 3, "ccc", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 4, "ddd", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 5, "eee", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 6, "fff", "2021-1-2")
    ));
    writeHive(unkeyedTable, HiveLocationKind.INSTANT, Lists.newArrayList(
        newRecord(unkeyedTable.schema(), 7, "aaa_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 8, "bbb_hive", "2021-1-1"),
        newRecord(unkeyedTable.schema(), 9, "ccc_hive", "2021-1-2"),
        newRecord(unkeyedTable.schema(), 10, "ddd_hive", "2021-1-2")
    ));
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(10, rows.size());
    assertContainIdSet(rows, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10);
    //disable arctic
    sql("set spark.arctic.sql.delegate.enable = false");
    sql("select * from {0}.{1} order by id", database, table);
    Assert.assertEquals(4, rows.size());
    assertContainIdSet(rows, 0, 7, 8, 9, 10);
    sql("set spark.arctic.sql.delegate.enable = true");
  }
}