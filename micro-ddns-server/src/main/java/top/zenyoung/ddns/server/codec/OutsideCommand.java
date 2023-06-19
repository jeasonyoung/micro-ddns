package top.zenyoung.ddns.server.codec;

/**
 * 外部(访问)-指令
 *
 * @author young
 */
public enum OutsideCommand {
    /**
     * 请求指令
     */
    OutsideReq,
    /**
     * 响应指令
     */
    OutsideRes,
    /**
     * 数据指令
     */
    OutsideData;
}
