package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.dto.PolicyQuestionAnalysis;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Component
public class AiPolicyQuestionAnalyzer {

    public PolicyQuestionAnalysis analyze(String question) {
        String q = question == null ? "" : question.trim();
        String lower = q.toLowerCase(Locale.ROOT);

        PolicyQuestionAnalysis result = new PolicyQuestionAnalysis();
        result.setOriginalQuestion(q);

        Set<String> codes = new LinkedHashSet<>();

        // 一、金融资格证书类
        if (lower.contains("cfa")) {
            codes.add("cfa");
        }
        if (lower.contains("frm")) {
            codes.add("frm");
        }
        if (lower.contains("acca")) {
            codes.add("acca");
        }
        if (lower.contains("fsa")) {
            codes.add("fsa");
        }
        if (lower.contains("cpa")) {
            codes.add("cpa");
        }
        if (q.contains("精算")) {
            codes.add("actuary");
        }
        if (q.contains("法律职业资格") || q.contains("法律资格")) {
            codes.add("legal_profession");
        }

        // 二、学历 / 职称类
if (q.contains("本科") || q.contains("学士")) {
    codes.add("bachelor");
}

if (q.contains("硕士") || q.contains("研究生")) {
    codes.add("master");
}

/*
 * 注意：
 * 1. 博士后属于 postdoctoral，不能误判成普通博士学位；
 * 2. 数据库条件索引使用 doctor_degree，不是 doctor。
 */
if ((q.contains("博士学位")
        || q.contains("博士研究生")
        || q.contains("博士"))
        && !q.contains("博士后")) {
    codes.add("doctor_degree");
}

if (q.contains("高级职称")
        || q.contains("正高级")
        || q.contains("副高级")
        || q.contains("高级专业技术职称")) {
    codes.add("senior_title");
}

        // 三、专业 / 行业类
        if (q.contains("软件工程") || q.contains("软件开发")) {
            codes.add("software_engineering");
        }
        if (q.contains("软件信息")) {
            codes.add("software_information");
        }
        if (q.contains("人工智能") || lower.contains("ai")) {
            codes.add("ai");
        }
        if (q.contains("集成电路")) {
            codes.add("integrated_circuit");
        }

        // 四、博士后类
        if (q.contains("博士后")) {
            codes.add("postdoctoral");
        }

        // 五、住房 / 安居 / 租购房类
        if (q.contains("住房") || q.contains("人才住房")) {
            codes.add("housing");
        }
        if (q.contains("住房补贴")) {
            codes.add("housing_subsidy");
        }
        if (q.contains("租房") || q.contains("租房补贴")) {
            codes.add("rent_subsidy");
        }
        if (q.contains("购房") || q.contains("购房补贴")) {
            codes.add("purchase_subsidy");
        }
        if (q.contains("安居")) {
            codes.add("settlement_housing");
        }
        if (q.contains("青年租赁住房")) {
            codes.add("youth_rental_housing");
        }
        if (q.contains("保障性商品房")) {
            codes.add("affordable_housing");
        }

        // 六、服务保障类
        if (q.contains("子女入学") || q.contains("孩子上学")) {
            codes.add("children_education");
        }
        if (q.contains("医疗保障") || q.contains("医疗保健")) {
            codes.add("medical_service");
        }

        List<String> conditionCodes = new ArrayList<>(codes);
        result.setConditionCodes(conditionCodes);
        result.setConditionReverseQuery(!conditionCodes.isEmpty() && isConditionReverseQuestion(q));
        result.setTargetSubject(resolveTargetSubject(q));

        return result;
    }

    private boolean isConditionReverseQuestion(String q) {
        return q.contains("可以申请")
                || q.contains("能申请")
                || q.contains("申请什么")
                || q.contains("可以申报")
                || q.contains("能申报")
                || q.contains("适合什么政策")
                || q.contains("有什么政策")
                || q.contains("有哪些政策")
                || q.contains("哪些政策")
                || q.contains("匹配什么政策")
                || q.contains("能享受什么")
                || q.contains("可以享受什么")
                || q.contains("有什么补助")
                || q.contains("有哪些补助")
                || q.contains("补助多少")
                || q.contains("补贴多少")
                || q.contains("有什么补贴")
                || q.contains("有哪些补贴")
                || q.contains("有什么支持")
                || q.contains("有哪些支持")
                || q.contains("支持政策")
                || q.contains("有什么待遇")
                || q.contains("有哪些待遇")
                || q.contains("怎么申请")
                || q.contains("如何申请")
                || q.contains("申请条件")
                || q.contains("有哪些")
                || q.contains("有什么");
    }

    private String resolveTargetSubject(String q) {
        if (q.contains("女儿") || q.contains("孩子") || q.contains("儿子")) {
            return "CHILD";
        }
        if (q.contains("朋友") || q.contains("客户") || q.contains("员工") || q.contains("同事")) {
            return "OTHER_PERSON";
        }
        return "USER_SELF_OR_UNCLEAR";
    }
}