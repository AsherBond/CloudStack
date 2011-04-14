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

package com.cloud.vm.dao;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.Transaction;
import com.cloud.vm.UserVmDetailVO;

@Local(value=UserVmDetailsDao.class)
public class UserVmDetailsDaoImpl extends GenericDaoBase<UserVmDetailVO, Long> implements UserVmDetailsDao {
    protected final SearchBuilder<UserVmDetailVO> VmSearch;
    protected final SearchBuilder<UserVmDetailVO> DetailSearch;

	protected UserVmDetailsDaoImpl() {
		VmSearch = createSearchBuilder();
		VmSearch.and("vmId", VmSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        VmSearch.done();
		
		DetailSearch = createSearchBuilder();
        DetailSearch.and("hostId", DetailSearch.entity().getVmId(), SearchCriteria.Op.EQ);
        DetailSearch.and("name", DetailSearch.entity().getName(), SearchCriteria.Op.EQ);
        DetailSearch.done();
	}
    
	@Override
	public void deleteDetails(long vmId) {
        SearchCriteria<UserVmDetailVO> sc = VmSearch.create();
        sc.setParameters("vmId", vmId);
        
        List<UserVmDetailVO> results = search(sc, null);
        for (UserVmDetailVO result : results) {
        	remove(result.getId());
        }		
	}

	@Override
	public UserVmDetailVO findDetail(long vmId, String name) {
        SearchCriteria<UserVmDetailVO> sc = DetailSearch.create();
        sc.setParameters("vmId", vmId);
        sc.setParameters("name", name);
		
        return findOneBy(sc);
	}

	@Override
	public Map<String, String> findDetails(long vmId) {
        SearchCriteria<UserVmDetailVO> sc = VmSearch.create();
        sc.setParameters("vmId", vmId);
        
        List<UserVmDetailVO> results = search(sc, null);
        Map<String, String> details = new HashMap<String, String>(results.size());
        for (UserVmDetailVO result : results) {
            details.put(result.getName(), result.getValue());
        }
        
        return details;
	}

	@Override
	public void persist(long vmId, Map<String, String> details) {
        Transaction txn = Transaction.currentTxn();
        txn.start();
        SearchCriteria<UserVmDetailVO> sc = VmSearch.create();
        sc.setParameters("vmId", vmId);
        expunge(sc);
        
        for (Map.Entry<String, String> detail : details.entrySet()) {
            UserVmDetailVO vo = new UserVmDetailVO(vmId, detail.getKey(), detail.getValue());
            persist(vo);
        }
        txn.commit();		
	}

}
