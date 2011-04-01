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
package com.cloud.vm;

import java.util.List;

import javax.ejb.Local;

import com.cloud.utils.db.GenericDaoBase;
import com.cloud.utils.db.SearchBuilder;
import com.cloud.utils.db.SearchCriteria;
import com.cloud.utils.db.SearchCriteria.Op;
import com.cloud.utils.time.InaccurateClock;
import com.cloud.vm.ItWorkVO.Step;
import com.cloud.vm.VirtualMachine.State;

@Local(value=ItWorkDao.class)
public class ItWorkDaoImpl extends GenericDaoBase<ItWorkVO, String> implements ItWorkDao {
    protected final SearchBuilder<ItWorkVO> AllFieldsSearch;
    protected final SearchBuilder<ItWorkVO> CleanupSearch;
    protected final SearchBuilder<ItWorkVO> OutstandingWorkSearch;
    protected final SearchBuilder<ItWorkVO> WorkInProgressSearch;
    
    protected ItWorkDaoImpl() {
        super();
        
        AllFieldsSearch = createSearchBuilder();
        AllFieldsSearch.and("instance", AllFieldsSearch.entity().getInstanceId(), Op.EQ);
        AllFieldsSearch.and("op", AllFieldsSearch.entity().getType(), Op.EQ);
        AllFieldsSearch.and("step", AllFieldsSearch.entity().getStep(), Op.EQ);
        AllFieldsSearch.done();
        
        CleanupSearch = createSearchBuilder();
        CleanupSearch.and("step", CleanupSearch.entity().getType(), Op.IN);
        CleanupSearch.and("time", CleanupSearch.entity().getUpdatedAt(), Op.LT);
        CleanupSearch.done();
        
        OutstandingWorkSearch = createSearchBuilder();
        OutstandingWorkSearch.and("instance", OutstandingWorkSearch.entity().getInstanceId(), Op.EQ);
        OutstandingWorkSearch.and("op", OutstandingWorkSearch.entity().getType(), Op.EQ);
        OutstandingWorkSearch.and("step", OutstandingWorkSearch.entity().getStep(), Op.NEQ);
        OutstandingWorkSearch.done();
        
        WorkInProgressSearch = createSearchBuilder();
        WorkInProgressSearch.and("server", WorkInProgressSearch.entity().getManagementServerId(), Op.EQ);
        WorkInProgressSearch.and("step", WorkInProgressSearch.entity().getStep(), Op.NIN);
        WorkInProgressSearch.done();
    }
    
    @Override
    public ItWorkVO findByOutstandingWork(long instanceId, State state) {
        SearchCriteria<ItWorkVO> sc = OutstandingWorkSearch.create();
        sc.setParameters("instance", instanceId);
        sc.setParameters("op", state);
        sc.setParameters("step", Step.Done);
        
        return findOneBy(sc);
    }
    
    @Override
    public void cleanup(long wait) {
        SearchCriteria<ItWorkVO> sc = CleanupSearch.create();
        sc.setParameters("step", Step.Done);
        sc.setParameters("time", InaccurateClock.getTimeInSeconds() - wait);
        
        remove(sc);
    }
    
    @Override
    public boolean update(String id, ItWorkVO work) {
        work.setUpdatedAt(InaccurateClock.getTimeInSeconds());
        
        return super.update(id, work);
    }
    
    @Override
    public boolean updateStep(ItWorkVO work, Step step) {
        work.setStep(step);
        return update(work.getId(), work);
    }
    
    @Override
    public List<ItWorkVO> listWorkInProgressFor(long nodeId) {
        SearchCriteria<ItWorkVO> sc = WorkInProgressSearch.create();
        sc.setParameters("server", nodeId);
        sc.setParameters("step", Step.Done);
        
        return search(sc, null);
        
    }
}
