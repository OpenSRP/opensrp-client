package org.ei.opensrp.dghs.HH_child;

import org.ei.opensrp.Context;
import org.ei.opensrp.commonregistry.AllCommonsRepository;
import org.ei.opensrp.domain.form.FormSubmission;
import org.ei.opensrp.service.formSubmissionHandler.FormSubmissionHandler;

import java.util.HashMap;
import java.util.Map;

public class child_followup_handler implements FormSubmissionHandler {


    public child_followup_handler() {

    }

    @Override
    public void handle(FormSubmission submission) {
        String entityID = submission.entityId();
        AllCommonsRepository memberrep = Context.getInstance().allCommonsRepositoryobjects("members");
        Map<String, String> ElcoDetails = new HashMap<String, String>();
        ElcoDetails.put("Is_Reg_Today","0");
//        ElcoDetails.put("FWELIGIBLE",submission.getFieldValue("FWELIGIBLE"));
        memberrep.mergeDetails(entityID,ElcoDetails);

    }
}
