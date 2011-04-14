/**
 *  Copyright (C) 2010 Cloud.com, Inc.  All rights reserved.
 * 
 * This software is licensed under the GNU General Public License v3 or later.
 * 
 * It is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package com.cloud.agent.api.storage;

import com.cloud.agent.api.storage.DownloadCommand.PasswordAuth;
import com.cloud.agent.api.to.TemplateTO;
import com.cloud.storage.Upload.Type;
import com.cloud.template.VirtualMachineTemplate;


public class UploadCommand extends AbstractUploadCommand {

	private TemplateTO template;
	private String url;
	private String installPath;	
	private boolean hvm;
	private String description;
	private String checksum;
	private PasswordAuth auth;
	private long templateSizeInBytes;
	private long id;
	private Type type;

	public UploadCommand(VirtualMachineTemplate template, String url, String installPath, long sizeInBytes) {
		
		this.template = new TemplateTO(template);
		this.url = url;
		this.installPath = installPath;
		this.checksum = template.getChecksum();
		this.id = template.getId();
		this.templateSizeInBytes = sizeInBytes;
		
	}
	
	public UploadCommand(String url, long id, long sizeInBytes, String installPath, Type type){
		this.template = null;
		this.url = url;
		this.installPath = installPath;
		this.id = id;
		this.type = type;
		this.templateSizeInBytes = sizeInBytes;
	}
	
	protected UploadCommand() {
	}
	
	public UploadCommand(UploadCommand that) {
		this.template = that.template;
		this.url = that.url;
		this.installPath = that.installPath;
		this.checksum = that.getChecksum();
		this.id = that.id;
	}

	public String getDescription() {
		return description;
	}


	public TemplateTO getTemplate() {
		return template;
	}

	public void setTemplate(TemplateTO template) {
		this.template = template;
	}

	@Override
    public String getUrl() {
		return url;
	}

	@Override
    public void setUrl(String url) {
		this.url = url;
	}

	public boolean isHvm() {
		return hvm;
	}

	public void setHvm(boolean hvm) {
		this.hvm = hvm;
	}

	public PasswordAuth getAuth() {
		return auth;
	}

	public void setAuth(PasswordAuth auth) {
		this.auth = auth;
	}

	public Long getTemplateSizeInBytes() {
		return templateSizeInBytes;
	}

	public void setTemplateSizeInBytes(Long templateSizeInBytes) {
		this.templateSizeInBytes = templateSizeInBytes;
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public void setInstallPath(String installPath) {
		this.installPath = installPath;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public void setChecksum(String checksum) {
		this.checksum = checksum;
	}

	public String getInstallPath() {
		return installPath;
	}

	public String getChecksum() {
		return checksum;
	}
}
