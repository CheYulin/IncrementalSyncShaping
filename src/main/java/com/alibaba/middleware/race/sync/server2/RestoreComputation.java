package com.alibaba.middleware.race.sync.server2;

import com.alibaba.middleware.race.sync.server2.operations.*;

import java.util.HashSet;
import java.util.concurrent.ExecutorService;

/**
 * Created by yche on 6/18/17.
 */
public class RestoreComputation {

    //    public static YcheHashMap recordMap = new YcheHashMap(20 * 1024 * 1024);
    public static HashSet<LogOperation> inRangeRecordSet = new HashSet<>();

    static void compute(final LogOperation[] logOperations) {
        DatabaseRestore.submitFirstPhase(logOperations);
        for (int i = 0; i < logOperations.length; i++) {
            LogOperation logOperation = logOperations[i];
            if (logOperation instanceof UpdateOperation) {
                // update
//                InsertOperation insertOperation = (InsertOperation) recordMap.get(logOperation); //2
//                insertOperation.mergeAnother((UpdateOperation) logOperation); //3

                if (logOperation instanceof UpdateKeyOperation) {
                    if (PipelinedComputation.isKeyInRange(logOperation.relevantKey)) {
                        inRangeRecordSet.remove(logOperation);
                    }

//                    insertOperation.changePK(((UpdateKeyOperation) logOperation).changedKey); //4
//                    recordMap.put(insertOperation); //5
                    LogOperation insertOperation = new LogOperation(((UpdateKeyOperation) logOperation).changedKey);
                    if (PipelinedComputation.isKeyInRange(insertOperation.relevantKey)) {
                        inRangeRecordSet.add(insertOperation);
                    }
                }
            } else if (logOperation instanceof DeleteOperation) {
                if (PipelinedComputation.isKeyInRange(logOperation.relevantKey)) {
                    inRangeRecordSet.remove(logOperation);
                }
            } else {
                // insert
//                recordMap.put(logOperation); //1
                if (PipelinedComputation.isKeyInRange(logOperation.relevantKey)) {
                    inRangeRecordSet.add(logOperation);
                }
            }
        }
        DatabaseRestore.submitSecondPhase();
    }

    // used by master thread
    static void parallelEvalAndSend(ExecutorService evalThreadPool) {
        BufferedEvalAndSendTask bufferedTask = new BufferedEvalAndSendTask();
        for (LogOperation logOperation : inRangeRecordSet) {
            if (bufferedTask.isFull()) {
                evalThreadPool.execute(bufferedTask);
                bufferedTask = new BufferedEvalAndSendTask();
            }
            bufferedTask.addData((InsertOperation) logOperation);
        }
        evalThreadPool.execute(bufferedTask);
    }
}
