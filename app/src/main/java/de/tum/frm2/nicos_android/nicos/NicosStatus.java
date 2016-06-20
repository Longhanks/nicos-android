//
// Copyright (C) 2016 Andreas Schulz <andreas.schulz@frm2.tum.de>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 US


package de.tum.frm2.nicos_android.nicos;

@SuppressWarnings("unused")
public class NicosStatus {
    /*idle*/
    public static final int STATUS_IDLEEXC  = -2;  // nothing started, last script raised exception

    /*idle*/
    public static final int STATUS_IDLE     = -1;  // nothing started

    /*running*/
    public static final int STATUS_RUNNING  = 0;   // execution running

    /*paused*/
    public static final int STATUS_INBREAK  = 1;   // execution halted, in break function

    /*running*/
    public static final int STATUS_STOPPING = 2;   // stop exception raised, waiting for propagation
}
