package com.alibaba.datax.plugin.reader.oscarreader;

import com.alibaba.datax.common.exception.DataXException;
import com.alibaba.datax.common.plugin.RecordSender;
import com.alibaba.datax.common.spi.Reader;
import com.alibaba.datax.common.util.Configuration;
import com.alibaba.datax.plugin.rdbms.reader.CommonRdbmsReader;
import com.alibaba.datax.plugin.rdbms.util.DBUtil;
import com.alibaba.datax.plugin.rdbms.util.DBUtilErrorCode;
import com.alibaba.datax.plugin.rdbms.util.DataBaseType;

import java.util.List;

public class OscarReader extends Reader {
    private static final DataBaseType DATABASE_TYPE = DataBaseType.Oscar;
    static {
    	//加载插件下面配置的驱动类
        DBUtil.loadDriverClass("reader", "oscar");
    }
    public static class Job extends Reader.Job {

        private Configuration originalConfig;
        private CommonRdbmsReader.Job commonRdbmsReaderMaster;

        @Override
        public void init() {
            this.originalConfig = super.getPluginJobConf();
            int fetchSize = this.originalConfig.getInt(
                    com.alibaba.datax.plugin.rdbms.reader.Constant.FETCH_SIZE,
                    Constant.DEFAULT_FETCH_SIZE);
            if (fetchSize < 1) {
                throw DataXException
                        .asDataXException(
                                DBUtilErrorCode.REQUIRED_VALUE,
                                String.format(
                                        "您配置的fetchSize有误，根据DataX的设计，fetchSize : [%d] 设置值不能小于 1.",
                                        fetchSize));
            }
            this.originalConfig.set(
                    com.alibaba.datax.plugin.rdbms.reader.Constant.FETCH_SIZE,
                    fetchSize);

            this.commonRdbmsReaderMaster = new SubCommonOscarReader.Job(
                    DATABASE_TYPE);
            this.commonRdbmsReaderMaster.init(this.originalConfig);
        }

        @Override
        public List<Configuration> split(int adviceNumber) {
            return this.commonRdbmsReaderMaster.split(this.originalConfig,
                    adviceNumber);
        }

        @Override
        public void post() {
            this.commonRdbmsReaderMaster.post(this.originalConfig);
        }

        @Override
        public void destroy() {
            this.commonRdbmsReaderMaster.destroy(this.originalConfig);
        }

    }

    public static class Task extends Reader.Task {

        private Configuration readerSliceConfig;
        private CommonRdbmsReader.Task commonRdbmsReaderSlave;

        @Override
        public void init() {
            this.readerSliceConfig = super.getPluginJobConf();
            this.commonRdbmsReaderSlave = new SubCommonOscarReader.Task(
                    DATABASE_TYPE);
            this.commonRdbmsReaderSlave.init(this.readerSliceConfig);
        }

        @Override
        public void startRead(RecordSender recordSender) {
            int fetchSize = this.readerSliceConfig
                    .getInt(com.alibaba.datax.plugin.rdbms.reader.Constant.FETCH_SIZE);

            this.commonRdbmsReaderSlave.startRead(this.readerSliceConfig,
                    recordSender, super.getTaskPluginCollector(), fetchSize);
        }

        @Override
        public void post() {
            this.commonRdbmsReaderSlave.post(this.readerSliceConfig);
        }

        @Override
        public void destroy() {
            this.commonRdbmsReaderSlave.destroy(this.readerSliceConfig);
        }
    }
}
