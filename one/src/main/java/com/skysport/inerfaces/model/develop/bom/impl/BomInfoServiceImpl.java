package com.skysport.inerfaces.model.develop.bom.impl;

import com.skysport.core.constant.CharConstant;
import com.skysport.core.exception.SkySportException;
import com.skysport.core.model.common.impl.CommonServiceImpl;
import com.skysport.core.model.workflow.IWorkFlowService;
import com.skysport.inerfaces.bean.develop.*;
import com.skysport.inerfaces.bean.form.develop.BomQueryForm;
import com.skysport.inerfaces.bean.relation.BomMaterialIdVo;
import com.skysport.inerfaces.bean.relation.ProjectItemBomIdVo;
import com.skysport.inerfaces.constant.WebConstants;
import com.skysport.inerfaces.constant.develop.ReturnCodeConstant;
import com.skysport.inerfaces.mapper.info.BomInfoMapper;
import com.skysport.inerfaces.model.develop.accessories.service.IAccessoriesService;
import com.skysport.inerfaces.model.develop.bom.IBomService;
import com.skysport.inerfaces.model.develop.bom.helper.BomHelper;
import com.skysport.inerfaces.model.develop.fabric.IFabricsService;
import com.skysport.inerfaces.model.develop.fabric.helper.FabricsServiceHelper;
import com.skysport.inerfaces.model.develop.packaging.service.IPackagingService;
import com.skysport.inerfaces.model.develop.product_instruction.IProductionInstructionService;
import com.skysport.inerfaces.model.develop.project.helper.ProjectHelper;
import com.skysport.inerfaces.model.develop.project.service.IProjectItemService;
import com.skysport.inerfaces.model.develop.quoted.helper.QuotedServiceHelper;
import com.skysport.inerfaces.model.develop.quoted.service.IFactoryQuoteService;
import com.skysport.inerfaces.model.develop.quoted.service.IQuotedService;
import com.skysport.inerfaces.model.permission.userinfo.service.IStaffService;
import com.skysport.inerfaces.model.relation.IRelationIdDealService;
import org.activiti.engine.runtime.ProcessInstance;
import org.apache.commons.collections.map.HashedMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * 类说明:
 * Created by zhangjh on 2015/7/13.
 */
@Service("bomManageService")
public class BomInfoServiceImpl extends CommonServiceImpl<BomInfo> implements IBomService, InitializingBean {

    @Resource(name = "bomInfoMapper")
    private BomInfoMapper bomInfoMapper;

    @Resource(name = "fabricsManageService")
    private IFabricsService fabricsManageService;

    @Resource(name = "accessoriesService")
    private IAccessoriesService accessoriesService;

    @Resource(name = "packagingService")
    private IPackagingService packagingService;

    @Resource(name = "quotedService")
    private IQuotedService quotedService;

    @Resource(name = "projectItemManageService")
    private IProjectItemService projectItemManageService;

    @Autowired
    private IRelationIdDealService bomMaterialServiceImpl;

    @Resource
    private IStaffService developStaffImpl;

    @Autowired
    private IWorkFlowService bomInfoTaskImpl;

    @Autowired
    private IProductionInstructionService productionInstructionServiceImpl;

    @Resource(name = "factoryQuoteService")
    private IFactoryQuoteService factoryQuoteService;

    @Override
    public void afterPropertiesSet() {
        commonMapper = bomInfoMapper;
    }

    @Override
    public int listFilteredInfosCounts(BomQueryForm bomQueryForm) {
        return bomInfoMapper.listFilteredInfosCounts(bomQueryForm);
    }

    @Override
    public List<BomInfo> searchInfos(BomQueryForm bomQueryForm) {
        return bomInfoMapper.searchInfos(bomQueryForm);
    }

    @Override
    public List<String> queryAllBomIdsByProjectId(String projectId) {
        return bomInfoMapper.queryAllBomIdsByProjectId(projectId);
    }


    /**
     * 查询bom信息
     *
     * @param bomId
     * @return
     */
    @Override
    public BomInfo queryInfoByNatrualKey(String bomId) {
        BomInfo bomInfo = super.queryInfoByNatrualKey(bomId);
        if (null != bomInfo) {

            //面料集合
            List<FabricsInfo> fabrics = fabricsManageService.queryFabricList(bomId);

            //辅料集合
            List<AccessoriesInfo> accessories = accessoriesService.queryAccessoriesList(bomId);

            //包材
            List<PackagingInfo> packagings = packagingService.queryPackagingList(bomId);

            //成衣厂 & 生产指示单
            List<FactoryQuoteInfo> factoryQuoteInfos = factoryQuoteService.queryFactoryQuoteInfoList(bomId);

            //报价信息
            QuotedInfo quotedInfo = quotedService.queryInfoByNatrualKey(bomId);

            KfProductionInstructionEntity productionInstruction = productionInstructionServiceImpl.queryInfoByNatrualKey(bomId);

            buildBomInfo(bomInfo, fabrics, accessories, packagings, factoryQuoteInfos, quotedInfo, productionInstruction);

        }
        return bomInfo;
    }

    /**
     * 更改子项目的颜色
     *
     * @param bomInfo
     */
    private void dealMainColor(BomInfo bomInfo) {
        String mainColor = bomInfo.getMainColor();
        String sexId = bomInfo.getSexId();
        String mainColorOld = bomInfo.getMainColorOld();
        if (StringUtils.isNotEmpty(mainColor) && StringUtils.isNotEmpty(mainColorOld) && StringUtils.isNotEmpty(sexId)) {
            if (!mainColor.trim().equals(mainColorOld.trim())) {
                projectItemManageService.updateMainColors(sexId, mainColor.trim(), mainColorOld.trim(), bomInfo.getProjectId());
            }
        } else {
            throw new SkySportException(ReturnCodeConstant.UPDATE_BOM_MAINCOLOR_PARAM_EXP);
        }
    }

    /**
     * 构造bom信息
     *
     * @param bomInfo
     * @param fabrics
     * @param accessories
     * @param packagings
     * @param factoryQuoteInfos
     */
    private void buildBomInfo(BomInfo bomInfo, List<FabricsInfo> fabrics, List<AccessoriesInfo> accessories, List<PackagingInfo> packagings, List<FactoryQuoteInfo> factoryQuoteInfos, QuotedInfo quotedInfo, KfProductionInstructionEntity productionInstruction) {

        bomInfo.setQuotedInfo(quotedInfo);
        bomInfo.setFabrics(fabrics);
        bomInfo.setAccessories(accessories);
        bomInfo.setPackagings(packagings);
        bomInfo.setFactoryQuoteInfos(factoryQuoteInfos);
        bomInfo.setProductionInstruction(productionInstruction);

    }

    /**
     * 修改bom信息
     *
     * @param bomInfo
     */
    @Override
    public void edit(BomInfo bomInfo) {
        String bomId = StringUtils.isEmpty(bomInfo.getBomId()) ? bomInfo.getNatrualkey() : bomInfo.getBomId();

        super.edit(bomInfo);

        //保存面料信息
        List<FabricsInfo> fabrics = fabricsManageService.updateOrAddBatch(bomInfo);

        //保存辅料信息
        List<AccessoriesInfo> accessories = accessoriesService.updateOrAddBatch(bomInfo);

        //保存包装材料信息
        List<PackagingInfo> packagings = packagingService.updateOrAddBatch(bomInfo);

        //保存成衣厂信息
        List<FactoryQuoteInfo> factoryQuoteInfos = factoryQuoteService.updateOrAddBatch(bomInfo);

        KfProductionInstructionEntity productionInstruction = productionInstructionServiceImpl.getInfoOrNeedtoAdd(bomId);

        //保存报价信息
        FabricsInfo fabricsInfo = getMainFabricsInfo(bomInfo);
        QuotedInfo quotedInfo = QuotedServiceHelper.getInstance().getQuotedInfo(bomInfo, fabricsInfo);
        quotedInfo = quotedService.updateOrAdd(quotedInfo);


        //如果颜色修改，需要修改bom的颜色(已在上面的修改bom方法中修改)和bom所属项目的子颜色
        dealMainColor(bomInfo);


        //增加Bom和物料的关系
        List<BomMaterialIdVo> bomMaterials = getBomMaterialIdVos(bomId, fabrics, accessories, packagings, factoryQuoteInfos, productionInstruction);

        bomMaterialServiceImpl.batchInsert(bomMaterials);

        buildBomInfo(bomInfo, fabrics, accessories, packagings, factoryQuoteInfos, quotedInfo, productionInstruction);

    }

    /**
     * 获取在价格表中需要显示的面料信息
     * 这一步操作一定在保存面料信息之后
     *
     * @param bomInfo
     * @return
     */
    public FabricsInfo getMainFabricsInfo(BomInfo bomInfo) {
        String bomId = bomInfo.getBomId();
        String seriesName = bomInfo.getSeriesName();
        List<FabricsInfo> fabricsInfos = fabricsManageService.queryAllFabricByBomId(bomId);//重新查一遍数据
        return FabricsServiceHelper.SINGLETONE.getMainFabricInfo(seriesName, fabricsInfos);
    }


    /**
     * 获取BOM关联的所有物料
     *
     * @param bomId
     * @param fabrics
     * @param accessories
     * @param packagings
     * @param factoryQuoteInfos
     * @param productionInstruction
     * @return
     */
    public List<BomMaterialIdVo> getBomMaterialIdVos(String bomId, List<FabricsInfo> fabrics, List<AccessoriesInfo> accessories, List<PackagingInfo> packagings, List<FactoryQuoteInfo> factoryQuoteInfos, KfProductionInstructionEntity productionInstruction) {
        List<BomMaterialIdVo> idsFabrics = BomHelper.getInstance().getBomMaterialIdVo(fabrics, bomId);
        List<BomMaterialIdVo> idsAccessoriesInfo = BomHelper.getInstance().getBomMaterialIdVo(accessories, bomId);
        List<BomMaterialIdVo> idsPackaging = BomHelper.getInstance().getBomMaterialIdVo(packagings, bomId);

        List<BomMaterialIdVo> idsFactoryQuoteInfo = BomHelper.getInstance().getBomMaterialIdVo(factoryQuoteInfos, bomId);
        List<BomMaterialIdVo> idsProInst = BomHelper.getInstance().getBomMaterialIdVo(productionInstruction, bomId);

        List<BomMaterialIdVo> bomMaterials = new ArrayList<>();
        bomMaterials.addAll(idsFabrics);
        bomMaterials.addAll(idsAccessoriesInfo);
        bomMaterials.addAll(idsPackaging);
        bomMaterials.addAll(idsFactoryQuoteInfo);
        bomMaterials.addAll(idsProInst);
        return bomMaterials;
    }


    @Override
    public List<BomInfo> selectAllBomSexAndMainColor(String projectId) {
        return bomInfoMapper.selectAllBomSexAndMainColor(projectId);
    }


    @Override
    public void delBomInThisIds(List<BomInfo> needDelBomList) {
        bomInfoMapper.delBomInThisIds(needDelBomList);
    }


    @Override
    public List<BomInfo> queryBomInfosByProjectItemIds(List<String> itemIds) {
        return bomInfoMapper.queryBomInfosByProjectItemIds(itemIds);
    }

    /**
     * 导出生产指示单，数量= sum（每个bom里面的成衣工厂）
     *
     * @param request
     * @param response
     * @param natrualkeys
     */
    @Override
    public void downloadProductinstruction(HttpServletRequest request, HttpServletResponse response, String natrualkeys) throws IOException, InvalidFormatException {
        List<String> itemIds = Arrays.asList(natrualkeys.split(CharConstant.COMMA));
        if (!itemIds.isEmpty()) {
            for (String bomId : itemIds) {
                BomInfo bomInfo = queryInfoByNatrualKey(bomId);
                BomHelper.downloadProductinstruction(bomInfo, response, request);
            }
        }


    }

    /**
     * 级联删除bom信息和project的颜色信息
     *
     * @param natrualKey
     */
    @Override
    public void delCacadBomInfo(String natrualKey) {
        //删除bom信息
        super.del(natrualKey);
        //删除对应子项目的性别颜色信息
        BomInfo info = queryInfoByNatrualKey(natrualKey);
        projectItemManageService.delSexColorInfoByBomInfo(info);
    }

    /**
     * 自动生成Bom信息，并保存DB
     * 修改子项目，处理bom的方式：
     * 已知数据库中子项目所有的款式（简称内部集合）和页面传入的所有款式(简称外部集合)；
     * 则需要修改的bom为：内部集合 与 外部集合的交集
     * 需要新增的bom为：外部集合 - 交集；
     * 需要删除的bom为:内部集合 - 交集；
     *
     * @param info 子项目信息
     * @author zhangjh
     */
    @Override
    public List<ProjectItemBomIdVo> autoCreateBomInfoAndSave(ProjectBomInfo info) {

        DealBomInfos dealBomInfos = new DealBomInfos(info).invoke();
        List<BomInfo> needDelBomList = dealBomInfos.getNeedDelBomList();
        List<BomInfo> needAddBomList = dealBomInfos.getNeedAddBomList();
        List<BomInfo> alls = dealBomInfos.getAlls();

        dealWorkFlow(needDelBomList, needAddBomList);


        //增加项目和子项目的关系
        List<ProjectItemBomIdVo> ids = ProjectHelper.SINGLETONE.getProjectItemBomIdVo(alls);
        return ids;
    }

    /**
     * 暂停或新启动
     * @param needDelBomList
     * @param needAddBomList
     */
    private void dealWorkFlow(List<BomInfo> needDelBomList, List<BomInfo> needAddBomList) {
        //终止部分流程，删除
        List<String> subtract = BomHelper.getInstance().buildBomIds(needDelBomList);

        List<ProcessInstance> instances = bomInfoTaskImpl.queryProcessInstancesActiveByBusinessKey(subtract);
        bomInfoTaskImpl.suspendProcessInstanceById(instances);//终止流程

        List<String> adds = BomHelper.getInstance().buildBomIds(needAddBomList);

        //启动流程
        startWorkFlow(adds);
    }

    @Override
    public void updateBatch(List<BomInfo> infos) {
        if (null != infos && !infos.isEmpty()) {
            for (BomInfo bomInfo : infos) {
                bomInfoMapper.updateInfo(bomInfo);
            }
        }

    }

    @Override
    public void updateApproveStatus(String businessKey, String status) {
        bomInfoMapper.updateApproveStatus(businessKey, status);
    }

    @Override
    public void updateApproveStatusBatch(List<String> businessKeys, String status) {
        bomInfoMapper.updateApproveStatusBatch(businessKeys, status);
    }

    @Override
    public void submit(String businessKey) {
        //启动流程
//        startWorkFlow(businessKey);
        //状态改为待审批
        updateApproveStatus(businessKey, WebConstants.APPROVE_STATUS_UNDO);
    }

    @Override
    public void submit(String taskId, String businessKey) {

        updateApproveStatus(businessKey, WebConstants.APPROVE_STATUS_UNDO);
    }

    /**
     * 启动开发流程
     *
     * @param bomId String
     */
    private void startWorkFlow(String bomId) {
        bomInfoTaskImpl.startProcessInstanceByBussKey(bomId);
    }

    public void startWorkFlow(List<String> bomIds) {
        for (String bomId : bomIds) {
            startWorkFlow(bomId);
        }
    }

    @Override
    public List<ProcessInstance> queryProcessInstancesActiveByBusinessKey(String natrualKey) {
        List<ProcessInstance> processInstances = bomInfoTaskImpl.queryProcessInstancesActiveByBusinessKey(natrualKey);
        return processInstances;
    }


    @Override
    public Map<String, Object> getVariableOfTaskNeeding(boolean approve) {
        Map<String, Object> variables = new HashedMap();
        String handleUserId;
        if (approve) {
            handleUserId = developStaffImpl.getManagerStaffGroupId();
            variables.put(WebConstants.DEVLOP_MANAGER, handleUserId);
        } else {
//            handleUserId = developStaffImpl.staffId();
//            variables.put(WebConstants.DEVLOP_STAFF, handleUserId);
        }
        variables.put(WebConstants.PROJECT_ITEM_PASS, approve);

        return variables;
    }

    private class DealBomInfos {
        private ProjectBomInfo info;
        private List<BomInfo> needDelBomList;
        private List<BomInfo> needAddBomList;
        private List<BomInfo> alls;

        public DealBomInfos(ProjectBomInfo info) {
            this.info = info;
        }

        public List<BomInfo> getNeedDelBomList() {
            return needDelBomList;
        }

        public List<BomInfo> getNeedAddBomList() {
            return needAddBomList;
        }

        public List<BomInfo> getAlls() {
            return alls;
        }

        public DealBomInfos invoke() {
            List<SexColor> sexColors = info.getSexColors();
            String projectId = info.getNatrualkey();
            String customerId = info.getCustomerId();
            String areaId = info.getAreaId();
            String seriesId = info.getSeriesId();
            List<BomInfo> allStyles = selectAllBomSexAndMainColor(projectId.trim());

            //获取需要更新的bom列表
            //交集
            List<BomInfo> intersection = BomHelper.getInstance().getIntersection(sexColors, allStyles);

            //获取需要删除的bom列表
            needDelBomList = BomHelper.getInstance().getNeedDelBomList(intersection, allStyles);

            //需要增加的bom列表
            needAddBomList = BomHelper.getInstance().getNeedAddBomList(intersection, sexColors, info, projectId, customerId, areaId, seriesId);

            alls = new ArrayList<>();
            alls.addAll(intersection);
            alls.addAll(needAddBomList);


            if (!needDelBomList.isEmpty()) {
                //删除
                delBomInThisIds(needDelBomList);
            }
            if (!intersection.isEmpty()) {
                //更新bom
                updateBatch(intersection);
            }
            //获取需要新增的bom列表
            if (!needAddBomList.isEmpty()) {
                //新增bom
                addBatch(needAddBomList);
            }
            return this;
        }
    }
}
