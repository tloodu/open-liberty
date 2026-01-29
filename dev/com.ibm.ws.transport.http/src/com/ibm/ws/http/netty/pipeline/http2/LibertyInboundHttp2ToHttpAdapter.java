/*******************************************************************************
 * Copyright (c) 2023, 2026 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package com.ibm.ws.http.netty.pipeline.http2;

import static io.netty.buffer.Unpooled.EMPTY_BUFFER;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.ibm.ws.ffdc.annotation.FFDCIgnore;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.FullHttpMessage;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http2.Http2Connection;
import io.netty.handler.codec.http2.Http2Error;
import io.netty.handler.codec.http2.Http2Exception;
import io.netty.handler.codec.http2.Http2Stream;
import io.netty.handler.codec.http2.HttpToHttp2ConnectionHandler;
import io.netty.handler.codec.http2.InboundHttp2ToHttpAdapter;

/**
 * An extension of {@link InboundHttp2ToHttpAdapter} for Liberty specific functionality.
 * Specifically the {@link processHeadersBegin()} for necessary header verification to
 * launch stream errors handled further in the pipeline to match the same behavior as
 * before.
 */
public class LibertyInboundHttp2ToHttpAdapter extends InboundHttp2ToHttpAdapter {

    private final Channel channel;
    private HttpToHttp2ConnectionHandler h2Handler;
    private AtomicInteger streamsActive = new AtomicInteger(0);
    private AtomicBoolean goAwayReceived = new AtomicBoolean(false);
    private AtomicBoolean goAwaySent = new AtomicBoolean(false);

    protected LibertyInboundHttp2ToHttpAdapter(Http2Connection connection, int maxContentLength, boolean validateHttpHeaders, boolean propagateSettings, Channel channel) {
        super(connection, maxContentLength, validateHttpHeaders, propagateSettings);
        this.channel = channel;
    }

    @Override
    @FFDCIgnore(NullPointerException.class)
    // Extended to properly get stream errors when working with header parsing with missing pesudo-headers
    protected io.netty.handler.codec.http.FullHttpMessage processHeadersBegin(ChannelHandlerContext ctx, io.netty.handler.codec.http2.Http2Stream stream,
                                                                              io.netty.handler.codec.http2.Http2Headers headers, boolean endOfStream, boolean allowAppend,
                                                                              boolean appendToTrailer) throws io.netty.handler.codec.http2.Http2Exception {
        try {
            boolean containsPath = Objects.nonNull(headers.path()) && !headers.path().toString().isEmpty();
            boolean containsScheme = Objects.nonNull(headers.scheme()) && !headers.scheme().toString().isEmpty();
            if (headers.method().toString().equalsIgnoreCase(HttpMethod.CONNECT.asciiName().toString())) {
                if (containsPath || containsScheme || Objects.isNull(headers.authority()))
                    throw new NullPointerException("Connect method request must omit path and scheme values!");
            } else {
                if (!containsPath)
                    throw new NullPointerException("Request path must have a value!");
                if (!containsScheme)
                    throw new NullPointerException("Request scheme must have a value!");
            }
            return super.processHeadersBegin(ctx, stream, headers, endOfStream, allowAppend, appendToTrailer);
        } catch (NullPointerException e) {
            throw Http2Exception.streamError(stream.id(), Http2Error.PROTOCOL_ERROR, e.getMessage());
        } catch (Exception e2) {
            throw e2;
        }
    }

    @Override
    public void onGoAwayRead(ChannelHandlerContext ctx, int lastStreamId, long errorCode, ByteBuf debugData) throws Http2Exception {
        goAwayReceived.getAndSet(true);
        super.onGoAwayRead(ctx, lastStreamId, errorCode, debugData);
        sendGoAwayIfClosing();
    }

    @Override
    public void onRstStreamRead(ChannelHandlerContext ctx, int streamId, long errorCode) throws Http2Exception {
        Http2Stream stream = connection.stream(streamId);
        FullHttpMessage msg = getMessage(stream);
        if (msg != null) {
            onRstStreamRead(stream, msg);
        }
        Http2Error code = Http2Error.valueOf(errorCode);
        if (Objects.isNull(code)) {
            code = Http2Error.INTERNAL_ERROR;
        }
        ctx.fireExceptionCaught(Http2Exception.streamError(streamId, code,
                                                           "HTTP/2 to HTTP layer caught stream reset"));
    }

    @Override
    public void onStreamActive(Http2Stream stream) {
        // Accumulate active streams
        streamsActive.incrementAndGet();
        super.onStreamActive(stream);
    }

    @Override
    public void onStreamClosed(Http2Stream stream) {
        // Remove inactive streams
        streamsActive.decrementAndGet();
        super.onStreamClosed(stream);
        sendGoAwayIfClosing();
    }

    protected void setHandler(HttpToHttp2ConnectionHandler h2Handler) {
        Objects.requireNonNull(h2Handler, "HttpToHttp2ConnectionHandler must not be null!");
        this.h2Handler = h2Handler;
    }

    private void sendGoAwayIfClosing() {
        Objects.requireNonNull(this.h2Handler, "HttpToHttp2ConnectionHandler must not be null for sending Go Away frame!");
        // We received a go away and need to check if we have finished the streams to close
        // the connection and send a go away back if we haven't sent one already
        if(goAwayReceived.get() && streamsActive.get() == 0 && goAwaySent.compareAndSet(false, true)) {
            h2Handler.encoder().writeGoAway(
                    channel.pipeline().lastContext(),
                    connection.remote().lastStreamCreated(),
                    Http2Error.NO_ERROR.code(),
                    EMPTY_BUFFER,
                    channel.newPromise());
            channel.flush();
        }
    }

}
