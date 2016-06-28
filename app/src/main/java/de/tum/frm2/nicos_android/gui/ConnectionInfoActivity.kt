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


package de.tum.frm2.nicos_android.gui

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.ListView
import android.widget.SimpleAdapter

import de.tum.frm2.nicos_android.R

class ConnectionInfoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_connection_info)
        val toolbar =  findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
        val connectionInfo = intent.getSerializableExtra(
                MainActivity.MESSAGE_DAEMON_INFO) as Map<*, *>
        val data = connectionInfo.map {
            mapOf("FIRST_LINE" to it.key.toString(), "SECOND_LINE" to it.value.toString())
        }
        val listView = findViewById(R.id.listView) as ListView
        listView.adapter = SimpleAdapter(
                this, data, android.R.layout.simple_list_item_2,
                arrayOf("FIRST_LINE", "SECOND_LINE"),
                intArrayOf(android.R.id.text1, android.R.id.text2))
    }
}
