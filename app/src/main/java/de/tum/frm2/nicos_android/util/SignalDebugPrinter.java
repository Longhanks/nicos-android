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


package de.tum.frm2.nicos_android.util;

@SuppressWarnings("unused")
public class SignalDebugPrinter implements NicosCallbackHandler {
    @Override
    public void handleSignal(String signal, Object data, Object args) {
        if (signal.equals("cache")) {
            return;
        }
        System.out.print(signal);
        if (data != null) {
            System.out.print(": " + data.toString());
            if (args != null) {
                System.out.println(", " + args.toString());
            }
            else {
                System.out.println();
            }
        }
        else {
            System.out.println();
        }
    }
}
