/**
 * Copyright (c) 2002-2011 "Neo Technology,"
 * Network Engine for Objects in Lund AB [http://neotechnology.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.impl.nioneo.store;

import java.io.IOException;
import java.io.Serializable;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages {@link org.neo4j.kernel.impl.nioneo.store.PersistenceWindow persistence windows} for a store. Each store
 * can configure how much memory it has for
 * {@link org.neo4j.kernel.impl.nioneo.store.MappedPersistenceWindow memory mapped windows}. This class tries to
 * make the most efficient use of those windows by allocating them in such a way
 * that the most frequently used records/blocks (be it for read or write
 * operations) are encapsulated by a memory mapped persistence window.
 */
public class TotallyMappedWindowPool implements WindowPool
{
    PersistenceWindow[] persistenceWindows;
    private int recordsPerWindow;
    private int recordLength;

    public TotallyMappedWindowPool( String storeName, int recordLength,
                                    FileChannel fileChannel, long mappedMem,
                                    boolean useMemoryMappedBuffers, boolean readOnly )
    {
        this.recordLength = recordLength;
        recordsPerWindow = Integer.MAX_VALUE / recordLength;
        try
        {
            long fileSize = fileChannel.size();
            long recordCount = fileSize / recordLength;
            int windowCount = (int) (recordCount / recordsPerWindow) + 1;
            persistenceWindows = new PersistenceWindow[windowCount];
            for (int iWindow = 0; iWindow < windowCount; iWindow++) {
                int startPosition = iWindow * recordsPerWindow;
                int windowSize = recordCount - startPosition < recordsPerWindow ?
                        (int) (recordCount - startPosition) * recordLength : recordsPerWindow * recordLength;
                persistenceWindows[iWindow] = new MappedPersistenceWindow( startPosition, recordLength, windowSize, fileChannel, FileChannel.MapMode.READ_ONLY );
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException( e );
        }
    }

    @Override
    public PersistenceWindow acquire( long position, OperationType operationType )
    {
        return persistenceWindows[((int) (position / recordsPerWindow ))];
    }

    @Override
    public void release( PersistenceWindow window )
    {
    }

    synchronized void close()
    {
    }


}