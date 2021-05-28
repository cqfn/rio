/*
 * MIT License Copyright (c) 2020-2021 cqfn.org
 * https://github.com/cqfn/rio/blob/master/LICENSE.txt
 */

package org.cqfn.rio.channel;

import java.io.IOException;
import java.nio.channels.Channel;

/**
 * Readable channel provider.
 * @param <C> Channel type
 * @since 0.2
 */
@FunctionalInterface
public interface ChannelSource<C extends Channel>  {
    /**
     * Create new channel.
     * @return Readable byte channel
     * @throws IOException On channel init error
     */
    C channel() throws IOException;
}

