/*******************************************************************************
 * Copyright (c) 2025 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package io.openliberty.http.netty.channel;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;

import io.netty.channel.Channel;

public class ReadOnlySocket extends Socket{

    private final InetSocketAddress local;
    private final InetSocketAddress remote;
    private final Channel nettyChannel;

    public ReadOnlySocket(Channel channel){
        this.nettyChannel = channel;
        this.local = (InetSocketAddress) channel.localAddress();
        this.remote = (InetSocketAddress) channel.remoteAddress();
    }

    @Override 
    public InetAddress getLocalAddress(){
        return local.getAddress();
    }

    @Override 
    public int getLocalPort(){
        return local.getPort();
    }

    @Override 
    public InetAddress getInetAddress(){
        return remote.getAddress();
    }

    @Override
    public int getPort(){
        return remote.getPort();
    }

    @Override
    public boolean isClosed(){
        return !nettyChannel.isActive();
    }

    //Ensure mutatable API calls are unsupported to avoid issues with the Netty socket

    @Override
    public void close(){} //NO OP

    @Override
    public void connect(SocketAddress endpoint){
        throwUnsupportedException();
    }

    @Override 
    public void connect(SocketAddress endpoint, int timeout){
        throwUnsupportedException();
    }

    @Override 
    public void bind(SocketAddress endpoint){
        throwUnsupportedException();
    }

    @Override 
    public void setSoTimeout(int timeout){
        throwUnsupportedException();
    }

    @Override 
    public void setTcpNoDelay(boolean value){
        throwUnsupportedException();
    }

    //TODO: This is just a POC, need to add any other fields that mutate the socket if this is a viable solution.


    private static void throwUnsupportedException() throws UnsupportedOperationException{
        throw new UnsupportedOperationException("Mutating the socket is not supported");
    }

}
