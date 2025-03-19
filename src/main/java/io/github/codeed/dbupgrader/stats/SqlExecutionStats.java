package io.github.codeed.dbupgrader.stats;

import lombok.Getter;

@Getter
public class SqlExecutionStats {
    private int updatedRecords = 0;
    private int insertedRecords = 0;
    private int deletedRecords = 0;
    
    public void addUpdatedRecords(int count) {
        this.updatedRecords += count;
    }
    
    public void addInsertedRecords(int count) {
        this.insertedRecords += count;
    }
    
    public void addDeletedRecords(int count) {
        this.deletedRecords += count;
    }
    
    public int getTotalAffectedRecords() {
        return updatedRecords + insertedRecords + deletedRecords;
    }

    public void reset() {
        updatedRecords = 0;
        insertedRecords = 0;
        deletedRecords = 0;
    }

    @Override
    public String toString() {
        return String.format("SqlExecutionStats{updated=%d, inserted=%d, deleted=%d, total=%d}",
                updatedRecords, insertedRecords, deletedRecords, getTotalAffectedRecords());
    }
} 