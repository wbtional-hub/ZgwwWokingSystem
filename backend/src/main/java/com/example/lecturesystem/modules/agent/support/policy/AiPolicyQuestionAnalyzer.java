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
        // 三、专业 / 行业类
if (q.contains("软件工程")
        || q.contains("软件工程专业")
        || q.contains("软件开发")
        || q.contains("计算机")
        || q.contains("信息技术")) {
    codes.add("software_engineering");
}

if (q.contains("软件信息")) {
    codes.add("software_information");
}

if (q.contains("电子信息")
        || q.contains("电子信息企业")
        || q.contains("电子信息产业")
        || q.contains("电子信息制造业")
        || q.contains("人工智能企业")
        || q.contains("软件信息企业")) {
    codes.add("electronic_org");
    codes.add("software_information");
}

if (q.contains("人工智能")
        || q.contains("人工智能项目")
        || q.contains("大模型")
        || q.contains("机器学习")
        || q.contains("深度学习")
        || lower.contains("ai")) {
    codes.add("ai");
}

if (q.contains("集成电路")) {
    codes.add("integrated_circuit");
}

if (q.contains("金融机构")
        || q.contains("基金管理机构")
        || q.contains("地方金融组织")
        || q.contains("金融投资集团")
        || q.contains("金融行业")
        || q.contains("金融单位")) {
    codes.add("finance_org");
}

if (q.contains("留厦")
        || q.contains("出站留厦")
        || q.contains("留在厦门")
        || q.contains("留厦就业")) {
    codes.add("stay_xiamen");
}

if (q.contains("特聘岗位")) {
    codes.add("special_post");
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
            || q.contains("申请哪些")
            || q.contains("想申请")
            || q.contains("可以申报")
            || q.contains("能申报")
            || q.contains("想申报")
            || q.contains("适合什么政策")
            || q.contains("适合哪个政策")
            || q.contains("适合哪些政策")
            || q.contains("适合什么人才政策")
            || q.contains("适合哪个人才政策")
            || q.contains("有什么政策")
            || q.contains("有哪些政策")
            || q.contains("哪些政策")
            || q.contains("匹配什么政策")
            || q.contains("匹配哪个政策")
            || q.contains("能享受什么")
            || q.contains("可以享受什么")
            || q.contains("可以同时享受")
            || q.contains("能否同时享受")
            || q.contains("能不能同时享受")
            || q.contains("可以同时看")
            || q.contains("同时看哪些")
            || q.contains("有什么补助")
            || q.contains("有哪些补助")
            || q.contains("哪些补助")
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
            || q.contains("可以吗")
            || q.contains("能不能")
            || q.contains("是否可以")
            || q.contains("有哪些")
            || q.contains("有什么");
}

    private String resolveTargetSubject(String q) {
    if (q.contains("我是")
            || q.contains("本人是")
            || q.contains("我有")
            || q.contains("我在")
            || q.contains("我想")
            || q.contains("我可以")
            || q.contains("我能")) {
        return "USER_SELF_OR_UNCLEAR";
    }

    if (q.contains("女儿")
            || q.contains("孩子")
            || q.contains("儿子")
            || q.contains("子女")) {
        return "CHILD";
    }

    if (q.contains("朋友")
            || q.contains("客户")
            || q.contains("同事")
            || q.contains("下属")
            || q.contains("员工可以")
            || q.contains("员工能")
            || q.contains("员工是否")) {
        return "OTHER_PERSON";
    }

    return "USER_SELF_OR_UNCLEAR";
}
}