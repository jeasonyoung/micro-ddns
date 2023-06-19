package top.zenyoung.ddns.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.ByteToMessageDecoder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;

/**
 * DDNS 通信协议编解码器(解码方向集成)
 *
 * @author young
 */
public class BaseInsideDecoder extends ByteToMessageDecoder {

    @Override
    protected void decode(final ChannelHandlerContext ctx, final ByteBuf in, final List<Object> out) {
        final ChannelPipeline p = ctx.pipeline();
        //添加编解码
        addPipelineCodecs(ctx, p);
        //移除当前
        p.remove(this);
    }

    protected final void addPipelineCodecs(@Nullable final ChannelHandlerContext ctx, @Nonnull final ChannelPipeline p) {
        if (Objects.nonNull(ctx)) {
            //1.挂载 协议解析
            p.addAfter(ctx.name(), "ddns-inside-codec", new InsideMessageCodec());
            return;
        }
        //1.挂载 协议解析
        p.addLast("ddns-inside-codec", new InsideMessageCodec());
    }
}
