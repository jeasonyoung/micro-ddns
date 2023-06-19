package top.zenyoung.ddns.server.codec;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import top.zenyoung.ddns.codec.BaseInsideDecoder;

/**
 * DDNS 服务端 编解码处理器
 *
 * @author young
 */
@Scope("prototype")
@Component("insideServerCodec")
public class InsideServerDecoder extends BaseInsideDecoder {

}
