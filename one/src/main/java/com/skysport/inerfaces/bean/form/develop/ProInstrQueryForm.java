package com.skysport.inerfaces.bean.form.develop;

import com.skysport.inerfaces.bean.develop.KfProductionInstructionEntity;
import com.skysport.inerfaces.bean.form.BaseQueyrForm;

/**
 * 说明:
 * Created by zhangjh on 2016-05-03.
 */
public class ProInstrQueryForm extends BaseQueyrForm {

    private KfProductionInstructionEntity productionInstr;

    public KfProductionInstructionEntity getProductionInstr() {
        return productionInstr;
    }

    public void setProductionInstr(KfProductionInstructionEntity productionInstr) {
        this.productionInstr = productionInstr;
    }
}
