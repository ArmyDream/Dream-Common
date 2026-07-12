package com.dream.common.core.enums;

/**
 * 文件存储 枚举
 *
 * @author dream
 */
public enum StorageType implements BaseEnum<String> {

    LOCAL("local", "Local", "本地存储"),
    MINIO("minio", "MinIO", "MinIO 对象存储"),
    FAST("fast", "FastDFS", "FastDFS"),
    ALIYUN("aliyun", "Aliyun OSS", "阿里云 OSS"),
    TENCENT("tencent", "Tencent COS", "腾讯云 COS"),
    QINIU("qiniu", "Qiniu Kodo", "七牛云 Kodo"),
    AWS("aws", "AWS S3", "AWS S3"),
    GOOGLE("google", "Google Cloud Storage", "谷歌云存储"),
    HUAWEI("huawei", "Huawei OBS", "华为云 OBS"),
    YANDEX("yandex", "Yandex Object Storage", "Yandex 对象存储"),
    ZHIFUBAO("zhifubao", "Zhifubao OSS", "支付宝 OSS"),
    BAIDU("baidu", "Baidu BOS", "百度云 BOS"),
    XIAOMI("xiaomi", "Xiaomi OSS", "小米云 OSS"),
    UPYUN("upyun", "UpYun", "又拍云"),
    CLOUDFLARE("cloudflare", "Cloudflare R2", "Cloudflare R2"),
    DOUYIN("douyin", "Douyin OSS", "抖音 OSS"),
    KUCOIN("kucoin", "Kucoin OSS", "Kucoin OSS"),
    DOUYIN_CLOUD("douyin_cloud", "Douyin Cloud", "抖音云"),
    ;

    private final String value;

    private final String labelEn;

    private final String labelZh;

    StorageType(String value, String labelEn, String labelZh) {
        this.value = value;
        this.labelEn = labelEn;
        this.labelZh = labelZh;
    }

    @Override
    public String getValue() {
        return value;
    }

    @Override
    public String getLabel() {
        return labelZh;
    }

    public String getLabelEn() {
        return labelEn;
    }

    public String getLabelZh() {
        return labelZh;
    }

    public String getLabel(String lang) {
        if ("en".equalsIgnoreCase(lang)) {
            return labelEn;
        }
        return labelZh;
    }

    @Override
    public String getColor() {
        switch (this) {
            case LOCAL:
                return "#8C8C8C";
            case MINIO:
                return "#FF4D4F";
            case FAST:
                return "#FAAD14";
            case ALIYUN:
                return "#FF6A00";
            case TENCENT:
                return "#1890FF";
            case QINIU:
                return "#52C41A";
            case AWS:
                return "#FF9900";
            case GOOGLE:
                return "#4285F4";
            case HUAWEI:
                return "#CF0A2C";
            case BAIDU:
                return "#2932E1";
            case CLOUDFLARE:
                return "#F38020";
            default:
                return "#13C2C2";
        }
    }

    public static StorageType fromValue(String value) {
        return BaseEnum.fromValue(StorageType.class, value);
    }
}
