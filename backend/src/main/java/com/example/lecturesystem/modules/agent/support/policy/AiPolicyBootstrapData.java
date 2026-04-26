package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.entity.AiPolicyAliasEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyEvalCaseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyFaqEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentAnswerEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyIntentPhraseEntity;
import com.example.lecturesystem.modules.agent.entity.AiPolicyRouteRuleEntity;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

final class AiPolicyBootstrapData {
    private AiPolicyBootstrapData() {
    }

    static List<AiPolicyAliasEntity> buildAliases(Long baseId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyAliasEntity> list = new ArrayList<>();
        addAlias(list, baseId, "xiamen_city", "double_hundred", "双百计划", 100, now);
        addAlias(list, baseId, "xiamen_city", "double_hundred", "双百", 98, now);
        addAlias(list, baseId, "xiamen_city", "double_hundred", "双百人才", 90, now);
        addAlias(list, baseId, "xiamen_city", "double_hundred", "厦门双百", 90, now);
        addAlias(list, baseId, "xiamen_city", "double_hundred", "创新团队", 85, now);
        addAlias(list, baseId, "xiamen_city", "double_hundred", "创业人才", 85, now);
        addAlias(list, baseId, "xiamen_city", "special_post", "特聘岗位", 100, now);
        addAlias(list, baseId, "xiamen_city", "special_post", "高层次人才特聘岗位", 90, now);
        addAlias(list, baseId, "xiamen_city", "special_fund", "专项资金", 100, now);
        addAlias(list, baseId, "xiamen_city", "special_fund", "创业扶持资金", 90, now);
        addAlias(list, baseId, "xiamen_city", "housing", "住房", 100, now);
        addAlias(list, baseId, "xiamen_city", "housing", "安居", 90, now);
        addAlias(list, baseId, "xiamen_city", "housing", "住房补贴", 90, now);
        addAlias(list, baseId, "xiamen_city", "housing", "人才住房", 92, now);
        addAlias(list, baseId, "xiamen_city", "housing", "安居政策", 88, now);
        addAlias(list, baseId, "xiamen_city", "ai_talent", "AI", 100, now);
        addAlias(list, baseId, "xiamen_city", "ai_talent", "人工智能", 100, now);
        addAlias(list, baseId, "xiamen_city", "ai_talent", "AI人才专项", 90, now);
        addAlias(list, baseId, "xiamen_city", "postdoc", "博士后", 100, now);
        addAlias(list, baseId, "xiamen_city", "postdoc", "工作站", 80, now);
        addAlias(list, baseId, "xiamen_city", "service_support", "服务保障", 100, now);
        addAlias(list, baseId, "xiamen_city", "service_support", "子女教育", 95, now);
        addAlias(list, baseId, "xiamen_city", "service_support", "医疗保障", 95, now);
        addAlias(list, baseId, "xiamen_city", "service_support", "平台申报", 85, now);
        addAlias(list, baseId, "fujian_province", "fujian_high_level", "高层次人才认定", 100, now);
        addAlias(list, baseId, "fujian_province", "fujian_recent_apply", "近期申报", 95, now);
        addAlias(list, baseId, "fujian_province", "fujian_recent_apply", "专项支持", 90, now);
        addAlias(list, baseId, "fujian_province", "fujian_bairen", "福建省百人计划", 100, now);
        addAlias(list, baseId, "fujian_province", "fujian_bairen", "引才百人计划", 90, now);
        addAlias(list, baseId, "fujian_province", "fujian_bairen", "创业创新人才项目", 85, now);
        return list;
    }

    static List<AiPolicyRouteRuleEntity> buildRouteRules(Long baseId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyRouteRuleEntity> list = new ArrayList<>();
        list.add(buildRule(baseId, "xiamen_city", "process", "双百计划|创新团队|创业人才", "topic", "process", 100, now));
        list.add(buildRule(baseId, "xiamen_city", "condition", "双百计划|特聘岗位|专项资金", "topic", "condition", 95, now));
        list.add(buildRule(baseId, "xiamen_city", "benefit", "双百计划|专项资金|住房|AI|博士后", "topic", "benefit", 95, now));
        list.add(buildRule(baseId, "xiamen_city", "service", "服务保障|子女教育|医疗保障|平台申报", "topic", "service", 100, now));
        list.add(buildRule(baseId, "fujian_province", "condition", "高层次人才认定|百人计划|近期申报|专项支持", "topic", "condition", 100, now));
        list.add(buildRule(baseId, null, "list", "人才政策|项目清单|有哪些", "main", "overview", 80, now));
        return list;
    }

    static List<AiPolicyFaqEntity> buildFaqs(Long baseId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyFaqEntity> list = new ArrayList<>();
        list.add(buildFaq(baseId, "xiamen_city", "double_hundred", "厦门市引进高层次创新创业人才“双百计划”实施意见", "overview", "双百计划|是什么", "双百计划是什么", "双百计划是厦门市引进高层次创新创业人才的重要专题政策，主要围绕创新个人、创新团队、创业人才等类别组织遴选和支持。", "双百计划专题（问答增强版）", 100, now));
        list.add(buildFaq(baseId, "xiamen_city", "double_hundred", "厦门市引进高层次创新创业人才“双百计划”实施意见", "target", "双百计划|支持哪些对象", "双百计划支持哪些对象", "双百计划主要支持创新个人、创新团队和创业人才等高层次创新创业类别，具体适用对象按对应申报类别分别审核。", "双百计划专题（问答增强版）", 100, now));
        list.add(buildFaq(baseId, "xiamen_city", "double_hundred", "厦门市引进高层次创新创业人才“双百计划”实施意见", "process", "双百计划|怎么申请|申请流程|申报流程", "双百计划怎么申请", "双百计划申请前要先区分申报类别。当前已能直接确认：创新团队、创业人才一般按组织申报、资格核查、部门联审、综合评审、研究确认五步推进；创新个人路径需结合岗位征集公告和专项办安排确认。", "双百计划专题（问答增强版）", 120, now));
        list.add(buildFaq(baseId, "xiamen_city", "double_hundred", "厦门市引进高层次创新创业人才“双百计划”实施意见", "benefit", "双百计划|补助多少|补贴多少|资助多少|支持金额", "双百计划补助多少", "按当前已命中的双百政策依据，可先直接回答：高层次创新人才每人最高100万元工作生活补助；领军型创业人才创业扶持资金一般为100万元至500万元，特别优秀项目可按“一事一议”获得1000万元至1亿元项目资助。若要继续区分创新个人、创新团队或不同创业阶段，再继续命中对应待遇条款。", "双百计划专题（问答增强版）", 115, now));
        list.add(buildFaq(baseId, "xiamen_city", "double_hundred", "厦门市引进高层次创新创业人才“双百计划”实施意见", "period", "双百计划|管理期多久", "双百计划管理期多久", "按当前专题依据可先明确：创新类一般3年，创业人才一般5年；顶峰人才通常参照创新类管理。如需继续确认管理期内考核、阶段兑现或退出机制，再继续命中对应专题条款。", "双百计划专题（问答增强版）", 96, now));
        list.add(buildFaq(baseId, "xiamen_city", "double_hundred", "厦门市引进高层次创新创业人才“双百计划”实施意见", "risk", "双百计划|退出|退出机制|追回|追回情形|终止支持|取消资格", "双百计划退出或追回情形有哪些", "按当前已命中的双百政策依据，可先明确几类高风险情形：因企业注销、人才离岗等导致项目终止的，不再继续享受相关政策待遇；主动放弃入选资格并经审核备案的，三年内不再受理该单位和个人申报项目；通过提供虚假材料、弄虚作假等方式入选的，将取消人才称号和再次参选资格，记入个人和企业征信记录，并追回相关支持。", "双百计划专题（问答增强版）", 112, now));
        list.add(buildFaq(baseId, "xiamen_city", "special_post", "厦门市高层次人才特聘岗位实施方案", "overview", "特聘岗位|是什么", "特聘岗位是什么", "特聘岗位是厦门市面向高层次人才设置的专项岗位支持安排，通常依托重点单位和岗位需求引进、使用高层次人才。", "特聘岗位专题（问答增强版）", 95, now));
        list.add(buildFaq(baseId, "xiamen_city", "special_post", "厦门市高层次人才特聘岗位实施方案", "process", "特聘岗位|怎么申报|申报流程", "特聘岗位怎么申报", "特聘岗位一般按岗位发布、单位申报、审核评审、确认入选推进，具体申报材料和年度时间节点仍以当年通知为准。", "特聘岗位专题（问答增强版）", 95, now));
        list.add(buildFaq(baseId, "xiamen_city", "special_fund", "厦门市高层次人才专项资金管理办法", "target", "专项资金|支持谁", "专项资金支持谁", "专项资金主要面向符合政策条件的高层次人才项目及其依托单位，不是所有产业项目都可以直接套用。", "专项资金专题（问答增强版）", 94, now));
        list.add(buildFaq(baseId, "xiamen_city", "special_fund", "厦门市高层次人才专项资金管理办法", "payment", "创业资金|怎么拨|拨付", "创业资金怎么拨", "创业资金一般按立项、实施进度和考核结果分阶段拨付，不是一次性无条件拨付；如需确认拨付节点，再看专项资金拨付条款和年度要求。", "专项资金专题（问答增强版）", 94, now));
        list.add(buildFaq(baseId, "xiamen_city", null, null, "list", "厦门|有哪些人才政策", "厦门有哪些人才政策", "厦门当前高频人才政策可先概括为统领政策、双百计划、特聘岗位、专项资金、住房、博士后、AI人才专项和服务保障等几类专题。", "厦门市人才政策（清洗导入版 v2）", 90, now));
        list.add(buildFaq(baseId, "xiamen_city", "ai_talent", "厦门市支持人工智能领域人才发展的若干措施", "ai", "厦门|AI|人工智能|人才专项", "厦门有 AI 人才专项吗", "有。厦门有人才专项围绕人工智能领域进行支持，AI问题应优先命中人工智能专题政策，不应先回无关产业项目。", "AI 专题", 98, now));
        list.add(buildFaq(baseId, "xiamen_city", "housing", "厦门市引进高层次人才住房补贴实施意见", "housing", "住房|安居|住房补贴|有哪些", "住房类人才政策有哪些", "住房类政策主要围绕人才住房、安居和住房补贴等专题展开，应优先按住房专题理解，不应混同产业补助。", "住房专题", 98, now));
        list.add(buildFaq(baseId, "xiamen_city", "service_support", "服务保障专题", "service", "服务保障|子女教育|医疗保障|平台申报", "人才服务保障包括什么", "人才服务保障通常包括子女教育、医疗保障、安居住房、落户和平台申报等配套服务，具体按对应服务保障专题落实。", "服务保障专题", 98, now));
        list.add(buildFaq(baseId, "fujian_province", "fujian_high_level", "福建省高层次人才认定与支持专题", "condition", "福建省|高层次人才|如何认定", "福建省高层次人才如何认定", "福建省高层次人才认定应重点看认定条件、认定程序和对应支持条款，认定问题应优先命中认定与支持专题。", "福建省高层次人才认定与支持专题", 98, now));
        list.add(buildFaq(baseId, "fujian_province", "fujian_recent_apply", "福建省近期申报与专项支持专题", "list", "福建省|近期|申报项目", "福建省近期有哪些申报项目", "福建省近期申报问题应优先查看近期申报与专项支持专题，重点关注在申报期内的项目清单和专项支持事项。", "福建省近期申报与专项支持专题", 95, now));
        list.add(buildFaq(baseId, "fujian_province", "fujian_bairen", "福建省引才“百人计划”专题", "benefit", "福建省|百人计划|补助多少", "福建省百人计划补助多少", "福建省百人计划补助不是统一单一金额，应按百人计划专题中的项目类别、支持阶段和待遇条款分别核对，避免混用厦门市级政策。", "福建省引才“百人计划”专题", 95, now));
        return list;
    }

    static List<AiPolicyEvalCaseEntity> buildEvalCases(Long baseId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyEvalCaseEntity> list = new ArrayList<>();
        addEval(list, baseId, "xiamen_city", "双百计划是什么", "double_hundred", "topic", "创新个人&&创新团队&&创业人才", "应以具体条款为准|进一步确认", now);
        addEval(list, baseId, "xiamen_city", "双百计划支持哪些对象", "double_hundred", "condition", "创新个人&&创新团队&&创业人才", "建议继续命中专题|进一步确认", now);
        addEval(list, baseId, "xiamen_city", "双百计划怎么申请", "double_hundred", "process", "组织申报&&资格核查&&部门联审&&综合评审&&研究确认", "无法确认|证据仍不足", now);
        addEval(list, baseId, "xiamen_city", "双百计划补助多少", "double_hundred", "benefit", "最高100万元&&100万元至500万元&&1000万元至1亿元", "不是统一一个金额|分类支持政策|建议继续命中专题块确认|应以具体条款为准", now);
        addEval(list, baseId, "xiamen_city", "双百计划管理期多久", "double_hundred", "risk", "创新类一般3年&&创业人才一般5年", "应以具体项目管理条款为准|建议继续命中管理期专题块确认", now);
        addEval(list, baseId, "xiamen_city", "双百计划退出或追回情形有哪些", "double_hundred", "risk", "企业注销&&人才离岗&&弄虚作假&&取消人才称号", "组织申报|资格核查|部门联审|综合评审|研究确认", now);
        addEval(list, baseId, "xiamen_city", "创业人才可以提现吗分阶段提现吗", "double_hundred", "benefit", "分阶段拨付", "自由提现|无法确认", now);
        addEval(list, baseId, "xiamen_city", "特聘岗位是什么", "special_post", "topic", "专项岗位支持安排", "进一步确认|建议继续命中专题", now);
        addEval(list, baseId, "xiamen_city", "特聘岗位怎么申报", "special_post", "process", "岗位发布&&单位申报&&审核评审&&确认入选", "仅能先看专题|无法确认", now);
        addEval(list, baseId, "xiamen_city", "专项资金支持谁", "special_fund", "condition", "高层次人才项目及其依托单位", "不能泛化替代其他产业项目|进一步确认", now);
        addEval(list, baseId, "xiamen_city", "创业资金怎么拨", "special_fund", "benefit", "分阶段拨付", "应优先命中专项资金拨付|无法确认", now);
        addEval(list, baseId, "xiamen_city", "厦门有哪些人才政策", null, "list", "双百计划&&特聘岗位&&专项资金&&住房&&博士后&&AI人才专项&&服务保障", "福建省", now);
        addEval(list, baseId, "xiamen_city", "厦门有 AI 人才专项吗", "ai_talent", "topic", "有。厦门有人才专项围绕人工智能领域进行支持", "无关产业项目|产业项目覆盖", now);
        addEval(list, baseId, "xiamen_city", "住房类人才政策有哪些", "housing", "list", "人才住房&&安居&&住房补贴", "产业补助|先回产业", now);
        addEval(list, baseId, "xiamen_city", "人才服务保障包括什么", "service_support", "service", "子女教育&&医疗保障&&安居住房&&平台申报", "产业项目|进一步确认", now);
        addEval(list, baseId, "fujian_province", "福建省高层次人才如何认定", "fujian_high_level", "condition", "认定条件&&认定程序&&支持条款", "厦门市|进一步确认", now);
        addEval(list, baseId, "fujian_province", "福建省近期有哪些申报项目", "fujian_recent_apply", "list", "项目清单&&专项支持事项", "厦门市|进一步确认", now);
        addEval(list, baseId, "fujian_province", "福建省百人计划补助多少", "fujian_bairen", "benefit", "不是统一单一金额", "厦门市|建议继续命中专题块确认", now);
        addEval(list, baseId, "unknown", "厦门和福建省的人才政策有什么区别", null, "compare", "厦门", null, now);
        return list;
    }

    static List<AiPolicyIntentEntity> buildIntents(Long baseId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyIntentEntity> list = new ArrayList<>();

        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.definition", "双百计划是什么", "双百计划是什么", "topic", "overview", "direct_confirmed", 120, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.target_group", "双百计划支持对象", "双百计划支持哪些对象", "condition", "condition", "partial_confirmed", 118, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.application_process", "双百计划申请流程", "双百计划怎么申请", "process", "process", "partial_confirmed", 125, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.subsidy", "双百计划补助标准", "双百计划补助多少", "benefit", "benefit", "partial_confirmed", 116, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.management_period", "双百计划管理期", "双百计划管理期多久", "risk", "management_period", "partial_confirmed", 112, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.payment", "双百计划拨付安排", "创业人才可以提现吗，分阶段提现吗", "benefit", "payment", "partial_confirmed", 108, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.risk_exit", "双百计划退出风险", "双百计划退出或追回情形有哪些", "risk", "risk", "partial_confirmed", 122, now);
        addIntent(list, baseId, "xiamen_city", "double_hundred", "double_hundred.service_support", "双百计划服务保障", "双百计划有哪些配套服务保障", "service", "service", "partial_confirmed", 98, now);

        addIntent(list, baseId, "xiamen_city", "special_post", "special_post.definition", "特聘岗位是什么", "特聘岗位是什么", "topic", "overview", "direct_confirmed", 110, now);
        addIntent(list, baseId, "xiamen_city", "special_post", "special_post.eligibility", "特聘岗位申报条件", "特聘岗位申报条件是什么", "condition", "condition", "partial_confirmed", 108, now);
        addIntent(list, baseId, "xiamen_city", "special_post", "special_post.process", "特聘岗位申报流程", "特聘岗位怎么申报", "process", "process", "partial_confirmed", 112, now);
        addIntent(list, baseId, "xiamen_city", "special_post", "special_post.subsidy", "特聘岗位支持待遇", "特聘岗位补助多少", "benefit", "benefit", "partial_confirmed", 104, now);
        addIntent(list, baseId, "xiamen_city", "special_post", "special_post.management_period", "特聘岗位管理期", "特聘岗位管理期多久", "risk", "management_period", "partial_confirmed", 100, now);

        addIntent(list, baseId, "xiamen_city", "special_fund", "special_fund.target_scope", "专项资金支持范围", "专项资金支持谁", "condition", "condition", "partial_confirmed", 108, now);
        addIntent(list, baseId, "xiamen_city", "special_fund", "special_fund.payment", "专项资金拨付安排", "创业资金怎么拨", "benefit", "payment", "partial_confirmed", 110, now);
        addIntent(list, baseId, "xiamen_city", "special_fund", "special_fund.usage_scope", "专项资金使用范围", "专项资金可以怎么用", "benefit", "benefit", "partial_confirmed", 102, now);
        addIntent(list, baseId, "xiamen_city", "special_fund", "special_fund.risk_exit", "专项资金风险退出", "专项资金有哪些退出或追回规则", "risk", "risk", "partial_confirmed", 98, now);

        addIntent(list, baseId, "xiamen_city", "housing", "housing.policy_overview", "住房类人才政策概览", "住房类人才政策有哪些", "list", "service", "direct_confirmed", 106, now);
        addIntent(list, baseId, "xiamen_city", "housing", "housing.eligibility_hint", "住房政策资格提示", "住房补贴申请条件是什么", "condition", "condition", "partial_confirmed", 100, now);
        addIntent(list, baseId, "xiamen_city", "housing", "housing.upgrade_rule", "住房政策升级衔接", "住房政策升级规则是什么", "risk", "service", "partial_confirmed", 92, now);

        addIntent(list, baseId, "xiamen_city", "ai_talent", "ai_talent.policy_overview", "AI人才政策概览", "厦门有AI人才专项吗", "topic", "overview", "direct_confirmed", 106, now);
        addIntent(list, baseId, "xiamen_city", "ai_talent", "ai_talent.ai_track_support", "AI赛道支持内容", "人工智能人才有哪些支持", "benefit", "benefit", "partial_confirmed", 100, now);
        addIntent(list, baseId, "xiamen_city", "ai_talent", "ai_talent.ai_special_post", "AI专项岗位支持", "AI人才有专项岗位支持吗", "service", "service", "partial_confirmed", 92, now);

        addIntent(list, baseId, "xiamen_city", "postdoc", "postdoc.policy_overview", "博士后政策概览", "厦门博士后政策有哪些", "topic", "overview", "direct_confirmed", 102, now);
        addIntent(list, baseId, "xiamen_city", "postdoc", "postdoc.station_support", "博士后工作站支持", "博士后工作站有哪些支持", "benefit", "benefit", "partial_confirmed", 96, now);
        addIntent(list, baseId, "xiamen_city", "postdoc", "postdoc.in_service_support", "博士后在站支持", "博士后在站补贴有哪些", "benefit", "benefit", "partial_confirmed", 96, now);

        addIntent(list, baseId, "xiamen_city", "service_support", "service_support.service_scope", "人才服务保障范围", "人才服务保障包括什么", "service", "service", "direct_confirmed", 106, now);
        addIntent(list, baseId, "xiamen_city", "service_support", "service_support.children_education", "人才子女教育保障", "人才子女教育保障怎么安排", "service", "service", "partial_confirmed", 96, now);
        addIntent(list, baseId, "xiamen_city", "service_support", "service_support.medical_support", "人才医疗保障", "人才医疗保障有哪些", "service", "service", "partial_confirmed", 96, now);
        addIntent(list, baseId, "xiamen_city", "service_support", "service_support.platform_apply", "人才平台申报", "人才平台怎么申报", "service", "service", "partial_confirmed", 94, now);

        addIntent(list, baseId, "fujian_province", "fujian_high_level", "fujian.high_level_recognition", "福建省高层次人才认定", "福建省高层次人才如何认定", "condition", "condition", "direct_confirmed", 105, now);
        addIntent(list, baseId, "fujian_province", "fujian_recent_apply", "fujian.recent_apply_projects", "福建省近期申报项目", "福建省近期有哪些申报项目", "list", "overview", "direct_confirmed", 103, now);
        addIntent(list, baseId, "fujian_province", "fujian_bairen", "fujian.bairen_subsidy", "福建省百人计划补助", "福建省百人计划补助多少", "benefit", "benefit", "partial_confirmed", 101, now);

        return list;
    }

    static List<AiPolicyIntentPhraseEntity> buildIntentPhrases(Long baseId, List<AiPolicyIntentEntity> intents) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyIntentPhraseEntity> list = new ArrayList<>();
        addIntentPhrase(list, baseId, intents, "double_hundred.definition", "双百计划是什么", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.definition", "双百是什么", "alias", 90, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.target_group", "双百计划支持哪些对象", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.target_group", "双百支持对象", "alias", 90, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.application_process", "双百计划怎么申请", "standard", 110, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.application_process", "双百计划申请流程", "alias", 108, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.application_process", "双百计划申报流程", "alias", 108, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.application_process", "创新团队申报流程", "alias", 96, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.application_process", "创业人才申报流程", "alias", 96, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.subsidy", "双百计划补助多少", "standard", 108, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.subsidy", "双百计划补贴多少", "alias", 102, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.subsidy", "双百计划资助多少", "alias", 102, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.subsidy", "双百计划支持金额", "alias", 96, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.management_period", "双百计划管理期多久", "standard", 96, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.payment", "创业人才可以提现吗", "alias", 92, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划退出或追回情形有哪些", "standard", 122, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划退出规则", "alias", 110, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划退出机制", "alias", 110, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划追回情形", "alias", 112, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划哪些情况会退出", "alias", 112, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划哪些情况会被追回", "alias", 112, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划终止支持", "alias", 106, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.risk_exit", "双百计划取消资格", "alias", 106, now);
        addIntentPhrase(list, baseId, intents, "double_hundred.service_support", "双百配套服务保障", "alias", 80, now);

        addIntentPhrase(list, baseId, intents, "special_post.definition", "特聘岗位是什么", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "special_post.eligibility", "特聘岗位申报条件", "standard", 96, now);
        addIntentPhrase(list, baseId, intents, "special_post.process", "特聘岗位怎么申报", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "special_post.subsidy", "特聘岗位补助多少", "alias", 92, now);
        addIntentPhrase(list, baseId, intents, "special_post.management_period", "特聘岗位管理期多久", "alias", 88, now);

        addIntentPhrase(list, baseId, intents, "special_fund.target_scope", "专项资金支持谁", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "special_fund.payment", "创业资金怎么拨", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "special_fund.usage_scope", "专项资金怎么用", "alias", 88, now);
        addIntentPhrase(list, baseId, intents, "special_fund.risk_exit", "专项资金退出规则", "alias", 82, now);

        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "住房类人才政策有哪些", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "住房", "alias", 110, now);
        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "安居", "alias", 100, now);
        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "住房补贴", "alias", 104, now);
        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "人才住房", "alias", 102, now);
        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "安居政策", "alias", 96, now);
        addIntentPhrase(list, baseId, intents, "housing.policy_overview", "住房政策", "hot_phrase", 88, now);
        addIntentPhrase(list, baseId, intents, "housing.eligibility_hint", "住房补贴申请条件", "alias", 92, now);
        addIntentPhrase(list, baseId, intents, "housing.upgrade_rule", "住房政策升级规则", "alias", 80, now);

        addIntentPhrase(list, baseId, intents, "ai_talent.policy_overview", "厦门有AI人才专项吗", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "ai_talent.policy_overview", "AI人才专项", "alias", 95, now);
        addIntentPhrase(list, baseId, intents, "ai_talent.ai_track_support", "人工智能人才有哪些支持", "standard", 96, now);
        addIntentPhrase(list, baseId, intents, "ai_talent.ai_special_post", "AI专项岗位支持", "alias", 82, now);

        addIntentPhrase(list, baseId, intents, "postdoc.policy_overview", "博士后政策有哪些", "standard", 98, now);
        addIntentPhrase(list, baseId, intents, "postdoc.station_support", "博士后工作站支持", "standard", 96, now);
        addIntentPhrase(list, baseId, intents, "postdoc.in_service_support", "博士后在站补贴", "standard", 96, now);

        addIntentPhrase(list, baseId, intents, "service_support.service_scope", "人才服务保障包括什么", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "service_support.children_education", "人才子女教育保障", "standard", 94, now);
        addIntentPhrase(list, baseId, intents, "service_support.medical_support", "人才医疗保障", "standard", 94, now);
        addIntentPhrase(list, baseId, intents, "service_support.platform_apply", "人才平台怎么申报", "standard", 92, now);

        addIntentPhrase(list, baseId, intents, "fujian.high_level_recognition", "福建省高层次人才如何认定", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "fujian.recent_apply_projects", "福建省近期有哪些申报项目", "standard", 100, now);
        addIntentPhrase(list, baseId, intents, "fujian.bairen_subsidy", "福建省百人计划补助多少", "standard", 100, now);
        return list;
    }

    static List<AiPolicyIntentAnswerEntity> buildIntentAnswers(Long baseId, List<AiPolicyIntentEntity> intents) {
        OffsetDateTime now = OffsetDateTime.now();
        List<AiPolicyIntentAnswerEntity> list = new ArrayList<>();
        for (AiPolicyIntentEntity intent : intents) {
            list.add(buildIntentAnswer(baseId, intent.getId(), "direct_confirmed", intent.getIntentName(),
                    resolveIntentAnswerTemplate(intent.getIntentCode(), "direct_confirmed"),
                    "strict_region,strict_policy", resolveFollowupSuggestion(intent.getIntentCode(), "direct_confirmed"), 100, now));
            list.add(buildIntentAnswer(baseId, intent.getId(), "partial_confirmed", intent.getIntentName(),
                    resolveIntentAnswerTemplate(intent.getIntentCode(), "partial_confirmed"),
                    "strict_region,topic_first", resolveFollowupSuggestion(intent.getIntentCode(), "partial_confirmed"), 90, now));
            list.add(buildIntentAnswer(baseId, intent.getId(), "insufficient_evidence", intent.getIntentName(),
                    resolveIntentAnswerTemplate(intent.getIntentCode(), "insufficient_evidence"),
                    "strict_region,evidence_required", resolveFollowupSuggestion(intent.getIntentCode(), "insufficient_evidence"), 80, now));
        }
        return list;
    }

    private static String resolveIntentAnswerTemplate(String intentCode, String answerMode) {
        if ("double_hundred.subsidy".equals(intentCode)) {
            return resolveDoubleHundredSubsidyTemplate(answerMode);
        }
        if ("double_hundred.management_period".equals(intentCode)) {
            return resolveDoubleHundredManagementPeriodTemplate(answerMode);
        }
        if ("double_hundred.risk_exit".equals(intentCode)) {
            return resolveDoubleHundredRiskExitTemplate(answerMode);
        }
        if ("insufficient_evidence".equalsIgnoreCase(answerMode)) {
            return "结论：{intent_name} 当前仍缺少足够专题证据，暂不把未命中的细节当作已确认事实。\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
        }
        String conclusion = switch (intentCode) {
            case "double_hundred.definition" -> "双百计划是厦门市引进高层次创新创业人才的重要专题政策，主要围绕创新个人、创新团队、创业人才等类别组织遴选和支持。";
            case "double_hundred.target_group" -> "双百计划主要支持创新个人、创新团队和创业人才等高层次创新创业类别，具体适用对象按对应申报类别分别审核。";
            case "double_hundred.application_process" -> "双百计划申请前要先区分申报类别。当前已能直接确认：创新团队、创业人才一般按组织申报、资格核查、部门联审、综合评审、研究确认五步推进；创新个人路径需结合岗位征集公告和专项办安排确认。";
            case "double_hundred.subsidy" -> "按当前已命中的双百政策依据，可先直接回答：高层次创新人才每人最高100万元工作生活补助；领军型创业人才创业扶持资金一般为100万元至500万元，特别优秀项目可按“一事一议”获得1000万元至1亿元项目资助。";
            case "double_hundred.payment" -> "创业人才资助通常按项目实施和考核安排分阶段拨付，不属于任意时点自由提现。";
            case "double_hundred.risk_exit" -> "按当前已命中的双百政策依据，可先明确：企业注销、人才离岗等导致项目终止的，不再继续享受政策待遇；主动放弃入选资格并备案的，三年内不再受理该单位和个人申报项目；弄虚作假入选的，将取消人才称号和再次参选资格，并追回相关支持。";
            case "double_hundred.service_support" -> "双百计划除资金支持外，还会衔接住房、子女教育、医疗保障、平台申报等配套服务，具体以对应服务保障专题落实。";
            case "special_post.definition" -> "特聘岗位是厦门市面向高层次人才设置的专项岗位支持安排，通常依托重点单位和岗位需求引进、使用高层次人才。";
            case "special_post.eligibility" -> "特聘岗位申报通常重点看设岗单位、岗位需求、申报对象层次和岗位匹配度，不是面向所有岗位普遍开放。";
            case "special_post.process" -> "特聘岗位一般按岗位发布、单位申报、审核评审、确认入选推进。";
            case "special_post.subsidy" -> "特聘岗位支持待遇通常与岗位类别、人才层次和考核安排挂钩，不是统一单一金额。";
            case "special_post.management_period" -> "特聘岗位管理通常要结合聘期、年度考核和续聘条件统筹判断。";
            case "special_fund.target_scope" -> "专项资金主要面向符合政策条件的高层次人才项目及其依托单位，不是所有产业项目都可以直接套用。";
            case "special_fund.payment" -> "创业资金一般按立项、实施进度和考核结果分阶段拨付，不是一次性无条件拨付。";
            case "special_fund.usage_scope" -> "专项资金应按管理办法用于约定的创新创业事项和项目支出，不能脱离批准用途随意使用。";
            case "special_fund.risk_exit" -> "如未按约定用途使用、未完成考核或出现违规情形，可能暂停拨付、追回资金或退出支持。";
            case "housing.policy_overview" -> "住房类政策主要围绕人才住房、安居和住房补贴等专题展开，应优先按住房专题理解，不应混同产业补助。";
            case "housing.eligibility_hint" -> "住房补贴或安居申请通常要看人才层次、在厦用人单位、住房状况及对应住房保障条件。";
            case "housing.upgrade_rule" -> "住房政策升级衔接通常按人才层次变化、政策更新和资格复核处理，不是自动无条件升级。";
            case "ai_talent.policy_overview" -> "有。厦门有人才专项围绕人工智能领域进行支持，AI问题应优先命中人工智能专题政策。";
            case "ai_talent.ai_track_support" -> "人工智能人才支持方向一般围绕人才引进、培养、岗位支持和项目承载平台等。";
            case "ai_talent.ai_special_post" -> "如涉及AI专项岗位支持，应结合人工智能专题与岗位类专题一并核对。";
            case "postdoc.policy_overview" -> "厦门有博士后相关政策，通常围绕博士后工作站、在站培养和相关补贴支持展开。";
            case "postdoc.station_support" -> "博士后工作站支持主要看设站平台资质、引进培养任务和平台承载条件。";
            case "postdoc.in_service_support" -> "博士后在站支持通常围绕在站补贴、科研支持和期满考核衔接，不是单一一笔补贴概念。";
            case "service_support.service_scope" -> "人才服务保障通常包括子女教育、医疗保障、安居住房、落户和平台申报等配套服务。";
            case "service_support.children_education" -> "子女教育保障通常看人才类别、随迁子女入学安排及属地配套政策。";
            case "service_support.medical_support" -> "医疗保障通常围绕就医便利、健康服务和相应保障衔接安排。";
            case "service_support.platform_apply" -> "平台申报一般依托人才服务平台或指定系统办理。";
            case "fujian.high_level_recognition" -> "福建省高层次人才认定重点看认定条件、认定程序和对应支持条款。";
            case "fujian.recent_apply_projects" -> "福建省近期申报问题应优先查看近期申报与专项支持专题，重点关注在申报期内的项目清单和专项支持事项。";
            case "fujian.bairen_subsidy" -> "福建省百人计划补助不是统一单一金额，应按百人计划专题中的项目类别、支持阶段和待遇条款分别核对。";
            default -> "{intent_name} 当前已命中可直接首答的专题依据。";
        };
        return "结论：" + conclusion + "\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
    }

    private static String resolveDoubleHundredManagementPeriodTemplate(String answerMode) {
        if ("insufficient_evidence".equalsIgnoreCase(answerMode)) {
            return "结论：双百计划管理期当前仍缺少足够专题证据，暂不把未命中的细节当作已确认事实。\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
        }
        String conclusion = "按当前专题依据可先明确：创新类一般3年，创业人才一般5年；顶峰人才通常参照创新类管理。";
        return "结论：" + conclusion + "\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
    }

    private static String resolveDoubleHundredSubsidyTemplate(String answerMode) {
        if ("insufficient_evidence".equalsIgnoreCase(answerMode)) {
            return "结论：双百计划补助标准当前仍缺少足够专题证据，暂不把未命中的细项金额当作已确认事实。\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
        }
        String conclusion = "按当前已命中的双百政策依据，可先直接回答：高层次创新人才每人最高100万元工作生活补助；领军型创业人才创业扶持资金一般为100万元至500万元，特别优秀项目可按“一事一议”获得1000万元至1亿元项目资助。";
        return "结论：" + conclusion + "\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
    }

    private static String resolveDoubleHundredRiskExitTemplate(String answerMode) {
        if ("insufficient_evidence".equalsIgnoreCase(answerMode)) {
            return "结论：双百计划退出或追回条款当前仍缺少足够专题证据，暂不把未命中的风险细项当作已确认事实。\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
        }
        String conclusion = "按当前已命中的双百政策依据，可先明确：因企业注销、人才离岗等导致项目终止的，不再继续享受政策待遇；主动放弃入选资格并经审核备案的，三年内不再受理该单位和个人申报项目；通过虚假材料、弄虚作假等方式入选的，将取消人才称号和再次参选资格，记入个人和企业征信记录，并追回相关支持。";
        return "结论：" + conclusion + "\n当前依据：{evidence_items}\n当前边界：{validation_summary}\n是否建议继续命中专题/原文：{followup_suggestion}";
    }

    private static String resolveFollowupSuggestion(String intentCode, String answerMode) {
        if ("insufficient_evidence".equalsIgnoreCase(answerMode)) {
            return "建议继续检索对应专题文档、年度公告或原始条款，再确认细项内容。";
        }
        return switch (intentCode) {
            case "double_hundred.application_process" -> "如需继续确认创新个人路径、材料清单、申报入口或年度时间节点，可继续命中双百计划流程专题和当年公告。";
            case "double_hundred.subsidy" -> "如需继续区分创新个人、创新团队或创业项目不同阶段的具体待遇，可继续命中双百计划待遇/支持标准专题逐项核对。";
            case "double_hundred.management_period" -> "如需继续确认管理期内考核、阶段兑现或退出机制，可继续命中对应专题条款。";
            case "double_hundred.payment" -> "如需继续确认拨付节点、兑现条件或考核要求，可继续命中拨付专题条款。";
            case "double_hundred.risk_exit" -> "如需继续核对不同风险情形下的处理口径、追回范围或信用影响，可继续命中双百计划退出机制专题和原始条款。";
            case "special_post.process" -> "如需继续确认材料清单、年度时间节点或具体审核环节，可继续命中特聘岗位流程专题和当年通知。";
            case "special_post.subsidy" -> "如需继续确认待遇标准，请继续命中特聘岗位支持待遇专题。";
            case "special_fund.payment" -> "如需继续确认拨付节点、兑现条件或考核要求，可继续命中专项资金拨付条款和年度要求。";
            case "special_fund.usage_scope" -> "如需继续确认可列支范围或负面清单，可继续命中专项资金使用条款。";
            case "housing.eligibility_hint" -> "如需继续确认可申请对象、住房状态要求或资格复核，请继续命中住房专题条款。";
            case "housing.upgrade_rule" -> "如需继续确认升级衔接情形或资格复核规则，可继续命中住房升级专题条款。";
            case "ai_talent.ai_track_support" -> "如需继续确认支持赛道、岗位或平台条款，可继续命中AI专题细分条款。";
            case "postdoc.station_support", "postdoc.in_service_support" -> "如需继续确认补贴标准、设站条件或在站考核要求，可继续命中博士后专题条款。";
            case "service_support.children_education", "service_support.medical_support", "service_support.platform_apply" -> "如需继续确认办理口径、平台入口或属地配套要求，可继续命中服务保障专题。";
            case "fujian.high_level_recognition" -> "如需继续确认认定层级、申报口径或支持项目，可继续命中认定与支持专题。";
            case "fujian.recent_apply_projects" -> "如需继续确认具体项目名称、申报窗口或当年公告口径，可继续命中近期申报专题。";
            case "fujian.bairen_subsidy" -> "如需继续确认具体金额和阶段安排，请继续命中百人计划待遇条款。";
            default -> "如需更细条款，可继续命中对应专题或原文。";
        };
    }

    private static void addAlias(List<AiPolicyAliasEntity> list, Long baseId, String regionScope, String policyKey, String alias, int priority, OffsetDateTime now) {
        AiPolicyAliasEntity entity = new AiPolicyAliasEntity();
        entity.setBaseId(baseId);
        entity.setRegionScope(regionScope);
        entity.setPolicyKey(policyKey);
        entity.setAlias(alias);
        entity.setPriority(priority);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        list.add(entity);
    }

    private static AiPolicyRouteRuleEntity buildRule(Long baseId, String regionScope, String questionType, String keywordPattern, String targetDocType, String targetTopicType, int priority, OffsetDateTime now) {
        AiPolicyRouteRuleEntity entity = new AiPolicyRouteRuleEntity();
        entity.setBaseId(baseId);
        entity.setRegionScope(regionScope);
        entity.setQuestionType(questionType);
        entity.setKeywordPattern(keywordPattern);
        entity.setTargetDocType(targetDocType);
        entity.setTargetTopicType(targetTopicType);
        entity.setPriority(priority);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private static AiPolicyFaqEntity buildFaq(Long baseId, String regionScope, String policyKey, String policyName, String slotCode, String questionPattern, String standardQuestion, String standardAnswer, String evidenceSource, int priority, OffsetDateTime now) {
        AiPolicyFaqEntity entity = new AiPolicyFaqEntity();
        entity.setBaseId(baseId);
        entity.setRegionScope(regionScope);
        entity.setPolicyKey(policyKey);
        entity.setPolicyName(policyName);
        entity.setSlotCode(slotCode);
        entity.setQuestionPattern(questionPattern);
        entity.setStandardQuestion(standardQuestion);
        entity.setStandardAnswer(standardAnswer);
        entity.setEvidenceSource(evidenceSource);
        entity.setPriority(priority);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private static void addEval(List<AiPolicyEvalCaseEntity> list, Long baseId, String regionScope, String question, String expectedPolicyKey, String expectedQuestionType, String expectedAnswerContains, String expectedForbiddenKeywords, OffsetDateTime now) {
        AiPolicyEvalCaseEntity entity = new AiPolicyEvalCaseEntity();
        entity.setBaseId(baseId);
        entity.setRegionScope(regionScope);
        entity.setQuestion(question);
        entity.setExpectedPolicyKey(expectedPolicyKey);
        entity.setExpectedQuestionType(expectedQuestionType);
        entity.setExpectedAnswerContains(expectedAnswerContains);
        entity.setExpectedForbiddenKeywords(expectedForbiddenKeywords);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        list.add(entity);
    }

    private static void addIntent(List<AiPolicyIntentEntity> list,
                                  Long baseId,
                                  String regionScope,
                                  String policyKey,
                                  String intentCode,
                                  String intentName,
                                  String standardQuestion,
                                  String questionType,
                                  String topicType,
                                  String answerLevelDefault,
                                  int priority,
                                                 OffsetDateTime now) {
        AiPolicyIntentEntity entity = new AiPolicyIntentEntity();
        entity.setBaseId(baseId);
        entity.setRegionScope(regionScope);
        entity.setPolicyKey(policyKey);
        entity.setIntentCode(intentCode);
        entity.setIntentName(intentName);
        entity.setStandardQuestion(standardQuestion);
        entity.setQuestionType(questionType);
        entity.setTopicType(topicType);
        entity.setAnswerLevelDefault(answerLevelDefault);
        entity.setPriority(priority);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        list.add(entity);
    }

    private static void addIntentPhrase(List<AiPolicyIntentPhraseEntity> list,
                                        Long baseId,
                                        List<AiPolicyIntentEntity> intents,
                                        String intentCode,
                                        String phrase,
                                        String phraseType,
                                        int hitWeight,
                                                         OffsetDateTime now) {
        Long intentId = findIntentId(intents, intentCode);
        if (intentId == null) {
            return;
        }
        AiPolicyIntentPhraseEntity entity = new AiPolicyIntentPhraseEntity();
        entity.setBaseId(baseId);
        entity.setIntentId(intentId);
        entity.setPhrase(phrase);
        entity.setPhraseType(phraseType);
        entity.setHitWeight(hitWeight);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        list.add(entity);
    }

    private static AiPolicyIntentAnswerEntity buildIntentAnswer(Long baseId,
                                                                Long intentId,
                                                                String answerMode,
                                                                String answerTitle,
                                                                String answerTemplate,
                                                                String evidenceRule,
                                                                String followupSuggestion,
                                                                int priority,
                                                         OffsetDateTime now) {
        AiPolicyIntentAnswerEntity entity = new AiPolicyIntentAnswerEntity();
        entity.setBaseId(baseId);
        entity.setIntentId(intentId);
        entity.setAnswerMode(answerMode);
        entity.setAnswerTitle(answerTitle);
        entity.setAnswerTemplate(answerTemplate);
        entity.setEvidenceRule(evidenceRule);
        entity.setFollowupSuggestion(followupSuggestion);
        entity.setPriority(priority);
        entity.setEnabled(Boolean.TRUE);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return entity;
    }

    private static Long findIntentId(List<AiPolicyIntentEntity> intents, String intentCode) {
        if (intents == null || intentCode == null) {
            return null;
        }
        for (AiPolicyIntentEntity intent : intents) {
            if (intentCode.equalsIgnoreCase(intent.getIntentCode())) {
                return intent.getId();
            }
        }
        return null;
    }
}
