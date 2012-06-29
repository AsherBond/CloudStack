//       Licensed to the Apache Software Foundation (ASF) under one
//       or more contributor license agreements.  See the NOTICE file
//       distributed with this work for additional information
//       regarding copyright ownership.  The ASF licenses this file
//       to you under the Apache License, Version 2.0 (the
//       "License"); you may not use this file except in compliance
//       with the License.  You may obtain a copy of the License at
//
//         http://www.apache.org/licenses/LICENSE-2.0
//
//       Unless required by applicable law or agreed to in writing,
//       software distributed under the License is distributed on an
//       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//       KIND, either express or implied.  See the License for the
//       specific language governing permissions and limitations
//       under the License.

package com.cloud.network.as;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import com.cloud.api.Identity;
import com.cloud.utils.db.GenericDao;

/**
 * @author Deepak Garg
 */


@Entity
@Table(name="counter")
public class CounterVO implements Counter, Identity {

	@Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name="id")
    private long id;

	@Column(name="source")
    @Enumerated(value=EnumType.STRING)
    private Source source;

    @Column(name="name")
    private String name;

    @Column(name="value", nullable=true)
    private int value;

    @Column(name="uuid")
    private String uuid;

    public CounterVO() {
    	this.uuid = UUID.randomUUID().toString();
    }

    @Column(name=GenericDao.REMOVED_COLUMN)
    Date removed;

    @Column(name=GenericDao.CREATED_COLUMN)
    Date created;

    public CounterVO(long id) {
        this.id = id;
    	this.uuid = UUID.randomUUID().toString();
    }

    public CounterVO(Source source, String name, int value) {
    	this.source = source;
    	this.name = name;
    	this.value = value;
    	this.uuid = UUID.randomUUID().toString();
    }

 	@Override
	public String getName() {
 		return name;
 	}

 	@Override
	public int getValue() {
 		return value;
 	}

	@Override
	public Source getSource() {
		return source;
	}

	public long getId() {
        return id;
    }

	@Override
	public String getUuid() {
    	return this.uuid;
    }

	public void setUuid(String uuid) {
    	this.uuid = uuid;
    }

    public Date getRemoved() {
        return removed;
    }

	public Date getCreated() {
		return created;
	}

}