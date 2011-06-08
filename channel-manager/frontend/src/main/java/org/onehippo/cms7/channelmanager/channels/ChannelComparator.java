package org.onehippo.cms7.channelmanager.channels;

import java.text.Collator;
import java.util.Comparator;

/**
 * Compares Channel titles with a {@link Collator} used for sorting the channels.
 *
 * TODO: Probably sort the channels by type?
 */
public class ChannelComparator implements Comparator<Channel> {
   @Override
    public int compare(Channel a, Channel b) {
        return Collator.getInstance().compare(a.getTitle(), b.getTitle());
    }
}
