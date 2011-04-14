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

package com.cloud.storage.dao;
import java.util.List;
import javax.ejb.Local;

import org.apache.log4j.Logger;

import com.cloud.storage.UploadVO;
import com.cloud.storage.Upload.Status;
import com.cloud.storage.Upload.Mode;
import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;

@Local(value={UploadDao.class})
public class UploadDaoImpl extends GenericDaoBase<UploadVO, Long> implements UploadDao {
	public static final Logger s_logger = Logger.getLogger(UploadDaoImpl.class.getName());
	protected final SearchBuilder<UploadVO> typeUploadStatusSearch;
	protected final SearchBuilder<UploadVO> typeHostAndUploadStatusSearch;
	protected final SearchBuilder<UploadVO> typeModeAndStatusSearch;
	
	protected static final String UPDATE_UPLOAD_INFO =
		"UPDATE upload SET upload_state = ?, upload_pct= ?, last_updated = ? "
	+   ", upload_error_str = ?, upload_job_id = ? "
	+   "WHERE host_id = ? and type_id = ? and type = ?";
	
	protected static final String UPLOADS_STATE_DC=
		"SELECT * FROM upload t, host h where t.host_id = h.id and h.data_center_id=? "
	+	" and t.type_id=? and t.upload_state = ?" ;
	
	
	public UploadDaoImpl() {
		typeUploadStatusSearch = createSearchBuilder();
		typeUploadStatusSearch.and("type_id", typeUploadStatusSearch.entity().getTypeId(), SearchCriteria.Op.EQ);
		typeUploadStatusSearch.and("upload_state", typeUploadStatusSearch.entity().getUploadState(), SearchCriteria.Op.EQ);
		typeUploadStatusSearch.and("type", typeUploadStatusSearch.entity().getType(), SearchCriteria.Op.EQ);
		typeUploadStatusSearch.done();
		
		typeHostAndUploadStatusSearch = createSearchBuilder();
		typeHostAndUploadStatusSearch.and("host_id", typeHostAndUploadStatusSearch.entity().getHostId(), SearchCriteria.Op.EQ);
		typeHostAndUploadStatusSearch.and("upload_state", typeHostAndUploadStatusSearch.entity().getUploadState(), SearchCriteria.Op.EQ);
		typeHostAndUploadStatusSearch.done();
		
		typeModeAndStatusSearch = createSearchBuilder();
		typeModeAndStatusSearch.and("mode", typeModeAndStatusSearch.entity().getMode(), SearchCriteria.Op.EQ);
		typeModeAndStatusSearch.and("upload_state", typeModeAndStatusSearch.entity().getUploadState(), SearchCriteria.Op.EQ);
		typeModeAndStatusSearch.done();
		
	}
	
	@Override
	public List<UploadVO> listByTypeUploadStatus(long typeId, UploadVO.Type type, UploadVO.Status uploadState) {
		SearchCriteria<UploadVO> sc = typeUploadStatusSearch.create();
		sc.setParameters("type_id", typeId);
		sc.setParameters("type", type);
		sc.setParameters("upload_state", uploadState.toString());
		return listBy(sc);
	}
	
	@Override
    public List<UploadVO> listByHostAndUploadStatus(long sserverId, Status uploadState){	    
        SearchCriteria<UploadVO> sc = typeHostAndUploadStatusSearch.create();
        sc.setParameters("host_id", sserverId);
        sc.setParameters("upload_state", uploadState.toString());
        return listBy(sc);
	}
	
	@Override
	public List<UploadVO> listByModeAndStatus(Mode mode, Status uploadState){
	    SearchCriteria<UploadVO> sc = typeModeAndStatusSearch.create();
	    sc.setParameters("mode", mode.toString());
	    sc.setParameters("upload_state", uploadState.toString());
	    return listBy(sc);
	}
}