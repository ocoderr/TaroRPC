package cn.iocoder.taro.rpc.core.rpc;

import cn.iocoder.taro.rpc.core.transport.Channel;
import cn.iocoder.taro.rpc.core.transport.Codec;
import com.alibaba.fastjson.JSON;

public class FastJSONCodec implements Codec {

    @Override
    public byte[] encodeBody(Channel channel, Object data) {
        return JSON.toJSONBytes(data);
    }

    @Override
    public Object decodeBody(Channel channel, byte[] bytes) {
        return JSON.parse(bytes);
    }

}
