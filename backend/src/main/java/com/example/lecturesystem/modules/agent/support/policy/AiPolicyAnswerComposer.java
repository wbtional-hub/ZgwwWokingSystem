package com.example.lecturesystem.modules.agent.support.policy;

import com.example.lecturesystem.modules.agent.support.policy.AiPolicyIntentClassifier.IntentType;
import com.example.lecturesystem.modules.knowledge.vo.KnowledgeSearchResultVO;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Service
public class AiPolicyAnswerComposer {
    public String compose(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                          AiPolicyRegionResolver.RegionMatch regionMatch,
                          AiPolicyIntentClassifier.IntentMatch intentMatch,
                          AiPolicyResolver.PolicyMatch policyMatch,
                          AiPolicyRouterService.RoutePlan routePlan,
                          AiPolicyEvidenceValidator.ValidationResult validationResult) {
        if (validationResult.answerMode() == AiPolicyEvidenceValidator.AnswerMode.INSUFFICIENT_EVIDENCE) {
            return buildInsufficientAnswer(regionMatch, intentMatch, policyMatch, routePlan, validationResult);
        }
        String standardizedTopicCardAnswer = buildStandardizedTopicCardAnswer(question, policyMatch, validationResult);
        if (standardizedTopicCardAnswer != null) {
            return standardizedTopicCardAnswer;
        }
        return switch (intentMatch.intentType()) {
            case LIST -> buildListAnswer(regionMatch, validationResult);
            case PROCESS -> buildProcessAnswer(policyMatch, validationResult);
            case CONDITION -> buildPointAnswer("结论：当前知识库已命中可确认的申报条件或适用对象。", validationResult, List.of("条件", "对象", "资格", "认定", "要求"));
            case BENEFIT -> buildPointAnswer("结论：当前知识库已命中可确认的支持待遇或资金安排。", validationResult, List.of("补助", "补贴", "奖励", "资助", "拨付", "兑付"));
            case SERVICE -> buildPointAnswer("结论：当前知识库已命中可确认的服务保障内容。", validationResult, List.of("服务保障", "子女教育", "医疗保障", "平台申报", "落户", "住房"));
            case RISK -> buildPointAnswer("结论：当前知识库已命中可确认的管理期或风险约束。", validationResult, List.of("管理期", "退出", "追回", "终止", "考核"));
            case COMPARE -> buildCompareAnswer(validationResult);
            default -> buildTopicAnswer(policyMatch, validationResult);
        };
    }

    private String buildListAnswer(AiPolicyRegionResolver.RegionMatch regionMatch,
                                   AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            builder.append(regionMatch.scope().getLabel()).append("当前命中的主要政策/专题如下。\n");
        } else {
            builder.append("当前命中的主要政策/专题如下。\n");
        }
        appendPolicyList(builder, validationResult.primaryHits());
        appendBoundary(builder, validationResult.validationSummary());
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildStandardizedTopicCardAnswer(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                                    AiPolicyResolver.PolicyMatch policyMatch,
                                                    AiPolicyEvidenceValidator.ValidationResult validationResult) {
        String policyKey = policyMatch == null ? null : policyMatch.policyKey();
        KnowledgeSearchResultVO card = firstStandardizedPhaseOneCard(policyKey, validationResult.primaryHits());
        if (card == null) {
            return null;
        }
        String content = normalizeCardContent(card.getSnippet());
        if (content.isBlank()) {
            return null;
        }
        String structuredAnswer = buildStructuredStandardizedTopicCardAnswer(question, policyKey, card, content);
        if (structuredAnswer != null && !structuredAnswer.isBlank()) {
            return structuredAnswer;
        }
        String fallbackContent = cleanStandardizedCardContent(content);
        if (fallbackContent == null || fallbackContent.isBlank()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        builder.append(fallbackContent).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private KnowledgeSearchResultVO firstStandardizedPhaseOneCard(String policyKey, List<KnowledgeSearchResultVO> hits) {
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        for (KnowledgeSearchResultVO hit : hits) {
            if (hit == null) {
                continue;
            }
            if ("POLICY_STANDARDIZATION_PHASE1".equalsIgnoreCase(hit.getScenePriority())
                    && isStandardizedDirectTopic(policyKey, hit.getTopicType())) {
                return hit;
            }
        }
        return null;
    }

    private boolean isStandardizedDirectTopic(String policyKey, String topicType) {
        if (topicType == null) {
            return false;
        }
        return switch (topicType.toLowerCase()) {
            case "material", "department", "boundary", "city_province_boundary", "station" -> true;
            case "process", "benefit" -> "xiamen_finance".equalsIgnoreCase(policyKey);
            default -> false;
        };
    }

    private String buildStructuredStandardizedTopicCardAnswer(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                                             String policyKey,
                                                             KnowledgeSearchResultVO card,
                                                             String content) {
        String topicType = card.getTopicType() == null ? "" : card.getTopicType().toLowerCase();
        return switch (topicType) {
            case "department" -> buildDepartmentCardAnswer(question, policyKey, card, content);
            case "process" -> "xiamen_finance".equalsIgnoreCase(policyKey)
                    ? buildFinanceProcessCardAnswer(card)
                    : null;
            case "benefit" -> "xiamen_finance".equalsIgnoreCase(policyKey)
                    ? buildFinanceBenefitCardAnswer(card)
                    : null;
            case "material" -> "xiamen_finance".equalsIgnoreCase(policyKey)
                    ? buildFinanceMaterialCardAnswer(card)
                    : buildMaterialCardAnswer(card, content);
            case "boundary", "city_province_boundary" -> buildBoundaryCardAnswer(card, content);
            case "station" -> buildStationCardAnswer(card, content);
            default -> null;
        };
    }

    private String buildDepartmentCardAnswer(AiPolicyQuestionNormalizer.NormalizedQuestion question,
                                             String policyKey,
                                             KnowledgeSearchResultVO card,
                                             String content) {
        if ("xiamen_finance".equalsIgnoreCase(policyKey)) {
            return buildFinanceDepartmentCardAnswer(card);
        }
        if (!"double_hundred".equalsIgnoreCase(policyKey)) {
            return buildGenericDepartmentCardAnswer(card, content);
        }
        String core = extractCardSection(content, "核心答复", "一、创新个人路径", "二、创新团队、创业人才路径", "办理建议", "当前边界", "依据边界");
        String personalPath = extractCardSection(content, "一、创新个人路径", "二、创新团队、创业人才路径", "办理建议", "当前边界", "依据边界");
        String teamPath = extractCardSection(content, "二、创新团队、创业人才路径", "办理建议", "当前边界", "依据边界");
        String advice = extractCardSection(content, "办理建议", "当前边界", "依据边界");
        String boundary = firstNonBlank(
                extractCardSection(content, "当前边界"),
                extractCardSection(content, "依据边界"),
                "年度入口、批次、附件模板、截止时间，以年度通知和遴选系统为准。");
        DepartmentFocus focus = resolveDepartmentFocus(buildQuestionText(question));
        if (focus == DepartmentFocus.STARTUP_TALENT) {
            return buildStartupTalentDepartmentAnswer(card, boundary);
        }
        if (focus == DepartmentFocus.INNOVATION_TEAM) {
            return buildInnovationTeamDepartmentAnswer(card, boundary);
        }
        if (focus == DepartmentFocus.INNOVATION_INDIVIDUAL) {
            return buildInnovationIndividualDepartmentAnswer(card, boundary);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("结论：该政策不是单一部门办理，应按申报类别和办理环节理解主管与责任主体。\n\n");
        builder.append("一、总体牵头\n");
        builder.append(firstNonBlank(extractSentence(core, List.of("专项办", "专项小组")),
                "总体上由专项办牵头组织，最终由专项小组研究审定。")).append("\n\n");
        builder.append("二、分类办理\n");
        builder.append("1. 创新个人：").append(firstNonBlank(personalPath,
                "主要通过高层次人才特聘岗位方式纳入，用人单位根据岗位需求自主引聘，经专项小组研究同意后纳入名单。")).append("\n");
        builder.append("2. 创新团队、创业人才：").append(firstNonBlank(teamPath,
                "通常按照专项办公告、资格核查、部门联审、综合评审、研究确认等流程推进。")).append("\n\n");
        builder.append("三、审核链条\n");
        builder.append(firstNonBlank(extractSentence(teamPath, List.of("资格核查", "部门联审", "综合评审", "研究确认")),
                "通常包括区/开发区核查，市人社局、市科技局分组联审，综合评审，以及专项小组审定。")).append("\n\n");
        builder.append("四、办理建议\n");
        builder.append(firstNonBlank(advice, "咨询时先区分申报类别：创新个人重点看特聘岗位路径，创新团队或创业人才重点看专项办公告、区/开发区核查、部门联审和专项小组审定。")).append("\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(boundary)).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildFinanceDepartmentCardAnswer(KnowledgeSearchResultVO card) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：金融服务产业人才项目不是个人脱离单位单独办理，应按“金融主管部门归口、所在机构推荐申报、主管部门审核确认”的口径理解。\n\n");
        builder.append("一、主管归口\n");
        builder.append("金融服务产业人才项目属于金融领域人才项目，归口和主管口径应重点关注市委金融办及当年度申报通知。\n\n");
        builder.append("二、申报和推荐主体\n");
        builder.append("个人通常需要依托所在机构申报或推荐。应先确认所在单位是否属于金融机构、基金管理机构、地方金融组织、金融投资集团等政策覆盖范围。\n\n");
        builder.append("三、审核确认\n");
        builder.append("具体受理渠道、审核流程、认定程序和兑现方式，以年度申报通知、主管部门审核和申报系统要求为准。\n\n");
        builder.append("四、办理建议\n");
        builder.append("咨询时先问清楚：所在机构类型、申报人身份、关注的是资格证书报考费补贴还是人才项目支持、是否满足在厦连续全职工作年限、是否由单位推荐申报。\n\n");
        builder.append("当前边界：年度入口、批次、截止时间、系统填报字段、附件模板和具体兑现口径属于动态事项，应以当年度正式通知和主管部门最新要求为准。\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildGenericDepartmentCardAnswer(KnowledgeSearchResultVO card, String content) {
        String core = extractCardSection(content, "核心答复", "办理建议", "当前边界", "依据边界");
        String advice = extractCardSection(content, "办理建议", "当前边界", "依据边界");
        String boundary = firstNonBlank(
                extractCardSection(content, "当前边界"),
                extractCardSection(content, "依据边界"),
                "具体受理部门、申报入口、材料模板和办理批次，以年度申报通知或主管部门最新要求为准。");
        StringBuilder builder = new StringBuilder();
        builder.append("结论：该问题应按具体政策的主管部门、受理单位、申报主体和审核链条分别核对，不能套用其他政策的办理部门。\n\n");
        builder.append("一、主管或归口口径\n");
        builder.append(firstNonBlank(core, "当前专题卡已命中主管部门类信息，但仍需结合正式政策文件或年度通知确认具体归口口径。")).append("\n\n");
        builder.append("二、办理建议\n");
        builder.append(firstNonBlank(advice, "建议先确认政策名称、申报类别、个人或单位申报主体，再核对年度通知中的主管部门、受理渠道、审核流程和咨询窗口。")).append("\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(boundary)).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildFinanceBenefitCardAnswer(KnowledgeSearchResultVO card) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：金融服务产业人才项目的支持内容要区分资格证书报考费补贴和人才服务保障等方向。其中，CFA、FRM、ACCA、FSA 等资格证书报考费补贴，符合条件的可按报考费给予累计不超过 5 万元补贴。\n\n");
        builder.append("一、资格证书报考费补贴\n");
        builder.append("取得 CFA、FRM、ACCA、FSA 等资格证书，并符合在厦连续全职工作满 2 年等要求的，可重点关注报考费补贴。\n\n");
        builder.append("二、适用机构范围\n");
        builder.append("需结合所在单位是否属于金融机构、基金管理机构、地方金融组织、金融投资集团等范围判断。\n\n");
        builder.append("三、不要混淆的事项\n");
        builder.append("证书报考费补贴与 B/C 类人才服务保障、其他人才项目支持不是同一件事，不能混为一谈。\n\n");
        builder.append("办理建议：如果问 CFA/FRM/ACCA/FSA，应核对证书、报考费、在厦连续全职工作满 2 年、所在机构类型、单位推荐和年度材料要求。\n\n");
        builder.append("当前边界：补贴金额、兑现周期、证书范围、材料模板、申报入口和年度批次以年度通知和主管部门审核为准。\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildFinanceProcessCardAnswer(KnowledgeSearchResultVO card) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：金融服务产业人才项目通常不是个人脱离单位单独申请，应先确认所在机构和申报事项，再由所在机构推荐或组织申报，按年度通知提交材料，经主管部门受理审核、认定确认后按规定兑现或享受支持。\n\n");
        builder.append("一、确认机构和申报事项\n");
        builder.append("先确认所在单位是否属于金融机构、基金管理机构、地方金融组织、金融投资集团等政策覆盖范围；再区分本次关注的是资格证书报考费补贴，还是人才项目支持、人才服务保障等事项。\n\n");
        builder.append("二、准备材料\n");
        builder.append("根据申报事项准备申报表、身份证明、学历学位、职称或资格证书、劳动关系或在厦工作证明、社保或个税、所在机构推荐材料、报考费凭证或业绩成果等材料。\n\n");
        builder.append("三、所在机构推荐申报\n");
        builder.append("个人通常需要依托所在机构推荐或组织申报，由单位按年度通知、主管部门要求和申报系统规则提交。\n\n");
        builder.append("四、主管部门受理审核\n");
        builder.append("材料提交后，通常进入受理、资格审核、评审或认定、公示确认等环节；具体审核口径以主管部门和年度通知为准。\n\n");
        builder.append("五、政策兑现或服务保障\n");
        builder.append("审核确认后，按规定兑现资格证书报考费补贴，或享受相应人才项目支持和服务保障。\n\n");
        builder.append("当前边界：申报入口、年度批次、截止时间、附件模板、系统字段和兑现时间以当年度正式申报通知、主管部门审核和申报系统要求为准。\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildFinanceMaterialCardAnswer(KnowledgeSearchResultVO card) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：金融服务产业人才项目材料应按申报事项准备。资格证书报考费补贴、人才项目支持、服务保障等事项的材料侧重点不同，最终以年度通知和申报系统为准。\n\n");
        builder.append("一、个人基础材料\n");
        builder.append("通常包括身份证明、个人基本信息、申报表等基础材料。\n\n");
        builder.append("二、学历、职称或资格证书材料\n");
        builder.append("根据申报事项准备学历学位证明、高级职称材料，以及 CFA、FRM、ACCA、FSA 等专业资格证书材料。\n\n");
        builder.append("三、在厦连续全职工作证明\n");
        builder.append("通常需要劳动合同、在岗证明、社保或个税、单位证明等，用于核验在厦连续全职工作情况；具体证明形式以年度通知为准。\n\n");
        builder.append("四、所在机构推荐材料\n");
        builder.append("所在金融机构、基金管理机构、地方金融组织、金融投资集团等单位的推荐意见、单位资质或承诺材料，需要按申报系统和主管部门要求准备。\n\n");
        builder.append("五、报考费补贴专项材料\n");
        builder.append("如申请资格证书报考费补贴，应重点准备证书材料、报考费凭证、支付记录、费用证明等。\n\n");
        builder.append("六、业绩成果或人才类别材料\n");
        builder.append("如申请人才项目支持或服务保障，应按类别准备业绩成果、岗位层级、项目贡献等证明材料。\n\n");
        builder.append("当前边界：材料清单、附件模板、盖章要求、线上字段、证明格式以当年度正式申报通知和申报系统要求为准。\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildStartupTalentDepartmentAnswer(KnowledgeSearchResultVO card, String boundary) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：创业人才通常走“双百计划”的申报遴选链条，重点不是特聘岗位路径，而是专项办组织申报、区/开发区核查、部门联审、综合评审和专项小组审定。\n\n");
        builder.append("一、谁牵头组织\n");
        builder.append("专项办发布申报公告，申报人或团队按要求通过遴选系统提交材料。\n\n");
        builder.append("二、谁做资格核查\n");
        builder.append("各区、开发区对申报人和项目开展书面审核与现场核查，重点核查基本资格、在岗履职、平台建设、项目推进等情况。\n\n");
        builder.append("三、谁联审和评审\n");
        builder.append("市人社局、市科技局按所属产业领域分组联审并提出审核意见，之后进入综合评审，通常包括材料评审、现场答辩、复核考察等环节。\n\n");
        builder.append("四、谁最终审定\n");
        builder.append("专项办根据评审与考察情况提出建议人选，报专项小组审定后确定正式入选名单。\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(firstNonBlank(boundary, "年度申报入口、批次、附件模板和截止时间，以年度通知和遴选系统要求为准。"))).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildInnovationTeamDepartmentAnswer(KnowledgeSearchResultVO card, String boundary) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：创新团队的重点是“谁审核”。通常先由区/开发区做资格核查，再由市人社局、市科技局按产业领域分组联审，经过综合评审后报专项小组审定。\n\n");
        builder.append("一、前置核查\n");
        builder.append("各区、开发区对创新团队和项目开展书面审核与现场核查，重点看基本资格、在岗履职、平台建设、项目推进等情况。\n\n");
        builder.append("二、部门联审\n");
        builder.append("市人社局、市科技局按所属产业领域进行分组联审，并提出审核意见。\n\n");
        builder.append("三、综合评审\n");
        builder.append("通常采取材料评审、现场答辩、复核考察等方式，形成初步人选或团队名单。\n\n");
        builder.append("四、研究确认\n");
        builder.append("专项办根据评审与考察情况提出建议名单，报专项小组审定后确定正式入选名单。\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(firstNonBlank(boundary, "年度申报入口、批次、附件模板和截止时间，以年度通知和遴选系统要求为准。"))).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildInnovationIndividualDepartmentAnswer(KnowledgeSearchResultVO card, String boundary) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：创新个人主要通过高层次人才特聘岗位方式纳入，执行链条应重点看专项办、用人单位和专项小组。\n\n");
        builder.append("一、专项办负责什么\n");
        builder.append("专项办牵头发布岗位征集公告，并研究确定特聘岗位数量。\n\n");
        builder.append("二、用人单位负责什么\n");
        builder.append("用人单位根据岗位需求自主引聘，具体岗位要求、引聘安排和承载条件以当年度岗位和单位要求为准。\n\n");
        builder.append("三、专项小组负责什么\n");
        builder.append("经专项小组研究同意后，符合要求的人选纳入创新个人名单。\n\n");
        builder.append("办理建议：如果问的是创新个人或创新人才，应优先核对当年度特聘岗位公告、设岗单位要求和专项小组确认口径；不要把创新团队、创业人才的申报遴选流程直接套用到创新个人。\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(firstNonBlank(boundary, "年度申报入口、批次、附件模板和截止时间，以年度通知和遴选系统要求为准。"))).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildMaterialCardAnswer(KnowledgeSearchResultVO card, String content) {
        String core = extractCardSection(content, "核心答复", "办理建议", "当前边界", "依据边界");
        String advice = extractCardSection(content, "办理建议", "当前边界", "依据边界");
        String boundary = firstNonBlank(
                extractCardSection(content, "当前边界"),
                extractCardSection(content, "依据边界"),
                "年度材料清单、附件模板和系统字段以正式通知为准。");
        StringBuilder builder = new StringBuilder();
        builder.append("结论：可先按材料方向准备，但正式材料清单以年度通知和申报系统为准。\n\n");
        builder.append("一、基础材料\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("身份证明", "申报表")),
                "通常包括身份证明、基础信息表或申报表等材料。")).append("\n\n");
        builder.append("二、学历职称或资格材料\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("学历", "职称", "资格证书", "专业资格")),
                "通常包括学历学位、职称资质、专业资格证书等证明材料。")).append("\n\n");
        builder.append("三、单位推荐或承载材料\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("单位推荐", "承载", "劳动关系", "在厦工作")),
                "通常包括单位推荐、劳动关系或在厦工作证明、承载单位相关材料。")).append("\n\n");
        builder.append("四、项目成果或业绩材料\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("项目成果", "业绩", "成果", "项目")),
                "如政策涉及项目或人才业绩，通常还需要项目成果、业绩证明或相关佐证。")).append("\n\n");
        builder.append("五、申报表、承诺书等附件\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("承诺书", "附件", "模板")),
                "申报表、承诺书和附件模板应按当年度通知或申报系统要求填写。")).append("\n\n");
        if (!core.isBlank() || !advice.isBlank()) {
            builder.append("办理建议：").append(firstNonBlank(advice, core)).append("\n");
        }
        builder.append("当前边界：").append(stripSectionPrefix(boundary)).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildBoundaryCardAnswer(KnowledgeSearchResultVO card, String content) {
        String core = extractCardSection(content, "核心答复", "办理建议", "当前边界", "依据边界");
        String advice = extractCardSection(content, "办理建议", "当前边界", "依据边界");
        String boundary = firstNonBlank(
                extractCardSection(content, "当前边界"),
                extractCardSection(content, "依据边界"),
                "具体能否叠加、能否同时申报，以正式政策和主管部门审核为准。");
        StringBuilder builder = new StringBuilder();
        builder.append("结论：市级政策与省级政策属于不同层级，不能混为一谈。\n\n");
        builder.append("一、政策层级\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("市级", "省级", "层级")),
                firstNonBlank(core, "厦门市政策和福建省政策应分别按各自政策口径理解。"))).append("\n\n");
        builder.append("二、适用范围\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("适用范围", "厦门", "福建")),
                "市级政策通常围绕厦门市范围内的人才、单位或项目；省级政策按福建省政策口径执行。")).append("\n\n");
        builder.append("三、主管体系\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("主管", "部门", "审核")),
                "不同层级政策对应的主管部门、审核口径和办理系统可能不同。")).append("\n\n");
        builder.append("四、支持事项\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("支持", "补助", "事项")),
                "支持事项、补助标准和申报条件应分别查对应政策原文，不能直接互相替代。")).append("\n\n");
        builder.append("五、办理建议\n");
        builder.append(firstNonBlank(advice, "先确认问题属于厦门市口径还是福建省口径，再核对是否允许叠加享受或重复申报。")).append("\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(boundary)).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String buildStationCardAnswer(KnowledgeSearchResultVO card, String content) {
        String core = extractCardSection(content, "核心答复", "办理建议", "当前边界", "依据边界");
        String advice = extractCardSection(content, "办理建议", "当前边界", "依据边界");
        String boundary = firstNonBlank(
                extractCardSection(content, "当前边界"),
                extractCardSection(content, "依据边界"),
                "具体设站条件、材料和流程以正式通知为准。");
        StringBuilder builder = new StringBuilder();
        builder.append("结论：博士后工作站申请主要面向设站单位，不是个人直接申请。\n\n");
        builder.append("一、申请主体\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("设站单位", "单位", "申请主体")),
                firstNonBlank(core, "申请主体通常是具备科研平台和博士后培养条件的单位。"))).append("\n\n");
        builder.append("二、单位条件\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("单位条件", "条件", "资质")),
                "重点关注单位资质、科研基础、博士后培养承载能力等要求。")).append("\n\n");
        builder.append("三、科研与管理能力\n");
        builder.append(firstNonBlank(extractSentence(content, List.of("科研", "管理", "平台")),
                "通常需要具备稳定科研项目、导师或团队支撑，以及博士后日常管理能力。")).append("\n\n");
        builder.append("四、办理建议\n");
        builder.append(firstNonBlank(advice, "由拟设站单位按正式通知准备材料并申报；个人进站问题应另按博士后进站流程咨询。")).append("\n\n");
        builder.append("当前边界：").append(stripSectionPrefix(boundary)).append("\n");
        appendStandardizedCardCitation(builder, card);
        return builder.toString().trim();
    }

    private String cleanStandardizedCardContent(String content) {
        if (content == null) {
            return "";
        }
        String cleaned = normalizeCardContent(content);
        cleaned = cleaned.replace("适用问题：", "适用场景：");
        cleaned = cleaned.replaceAll("适用场景：[^。；;\\n]*(。|；|;|\\n)", "");
        cleaned = cleaned.replace("核心答复：", "\n结论：");
        cleaned = cleaned.replace("依据边界：", "\n当前边界：");
        cleaned = cleaned.replace("办理建议：", "\n办理建议：");
        return cleaned.trim();
    }

    private String normalizeCardContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String extractCardSection(String content, String label, String... nextLabels) {
        if (content == null || label == null) {
            return "";
        }
        String normalized = normalizeCardContent(content);
        int start = normalized.indexOf(label + "：");
        int labelLength = (label + "：").length();
        if (start < 0) {
            start = normalized.indexOf(label + ":");
            labelLength = (label + ":").length();
        }
        if (start < 0) {
            return "";
        }
        int valueStart = start + labelLength;
        int end = normalized.length();
        if (nextLabels != null) {
            for (String nextLabel : nextLabels) {
                if (nextLabel == null || nextLabel.isBlank()) {
                    continue;
                }
                int nextIndex = normalized.indexOf(nextLabel + "：", valueStart);
                if (nextIndex < 0) {
                    nextIndex = normalized.indexOf(nextLabel + ":", valueStart);
                }
                if (nextIndex >= 0 && nextIndex < end) {
                    end = nextIndex;
                }
            }
        }
        return stripSectionPrefix(normalized.substring(valueStart, end));
    }

    private String stripSectionPrefix(String text) {
        if (text == null) {
            return "";
        }
        return text.trim()
                .replaceFirst("^(适用场景|适用问题|核心答复|依据边界|当前边界|办理建议)[：:]\\s*", "")
                .trim();
    }

    private String extractSentence(String content, List<String> keywords) {
        if (content == null || content.isBlank() || keywords == null || keywords.isEmpty()) {
            return "";
        }
        String normalized = normalizeCardContent(content)
                .replace('\n', ' ')
                .replaceAll("\\s+", " ");
        String[] sentences = normalized.split("(?<=[。；;])");
        for (String sentence : sentences) {
            String trimmed = stripSectionPrefix(sentence);
            if (trimmed.isBlank() || trimmed.startsWith("适用场景")) {
                continue;
            }
            for (String keyword : keywords) {
                if (keyword != null && !keyword.isBlank() && trimmed.contains(keyword)) {
                    return trimmed;
                }
            }
        }
        return "";
    }

    private void appendStandardizedCardCitation(StringBuilder builder, KnowledgeSearchResultVO card) {
        builder.append("依据：POLICY_STANDARDIZATION_PHASE1 标准化专题卡");
        String citation = firstNonBlank(card.getDocTitle(), card.getPolicyName(), "政策专题卡")
                + " / "
                + firstNonBlank(card.getHeadingPath(), card.getSectionTitle(), card.getTopicType(), "专题卡");
        builder.append("（").append(citation).append("）。");
    }

    private String buildProcessAnswer(AiPolicyResolver.PolicyMatch policyMatch,
                                      AiPolicyEvidenceValidator.ValidationResult validationResult) {
        if (policyMatch != null && "double_hundred".equals(policyMatch.policyKey())) {
            return buildDoubleHundredProcessAnswer(validationResult);
        }
        StringBuilder builder = new StringBuilder();
        builder.append("结论：");
        builder.append("当前知识库已命中可确认的申请/申报流程依据。\n");
        builder.append("当前依据：\n");
        List<String> steps = extractProcessSteps(validationResult.primaryHits());
        if (steps.isEmpty()) {
            builder.append("1. 当前仅命中到部分流程线索，尚缺少足以展开完整步骤的专题证据。\n");
        } else {
            for (int i = 0; i < steps.size(); i++) {
                builder.append(i + 1).append(". ").append(steps.get(i)).append("\n");
            }
        }
        builder.append("当前边界：具体材料清单、申报入口、年度时间节点，仍以当年申报公告和遴选系统要求为准。\n");
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildDoubleHundredProcessAnswer(AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：“双百计划”申报流程要先区分申报类别，不能简单按一个统一流程理解。\n");
        builder.append("申报路径：\n");
        builder.append("1. 创新个人：通常通过高层次人才特聘岗位方式遴选，具体遴选程序、设岗单位要求和年度批次，以当年度通知及设岗单位要求为准。\n");
        builder.append("2. 创新团队、创业人才：一般按组织申报、资格核查、部门联审、综合评审、研究确认等步骤推进。\n");
        builder.append("办理建议：先确认申报类别，再核对当年度申报通知中的入口、材料、主管部门、批次安排和系统填报要求。\n");
        builder.append("当前边界：具体申报时间、申报入口、材料清单、主管部门和年度批次，必须以当年度正式申报通知为准；当前知识库未命中的内容不能编造。\n");
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildCompareAnswer(AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：厦门市级与福建省级人才政策属于不同层级，回答时应分别以各自地域专题为准，再进行并列比较。\n");
        Map<String, List<KnowledgeSearchResultVO>> grouped = validationResult.groupByRegion();
        appendRegionBlock(builder, "厦门市级", grouped.get("xiamen_city"));
        appendRegionBlock(builder, "福建省级", grouped.get("fujian_province"));
        builder.append("当前边界：若要继续比较申报条件、补助标准或办理流程，建议分别继续命中对应地区的专题文档。\n");
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildTopicAnswer(AiPolicyResolver.PolicyMatch policyMatch,
                                    AiPolicyEvidenceValidator.ValidationResult validationResult) {
        String title = policyMatch != null && policyMatch.canonicalPolicyName() != null
                ? policyMatch.canonicalPolicyName()
                : "相关政策专题";
        StringBuilder builder = new StringBuilder();
        builder.append("结论：当前知识库已命中 ").append(title).append(" 的可确认内容。\n");
        appendPointBlock(builder, "当前依据", extractPoints(validationResult.primaryHits(), List.of("支持", "对象", "措施", "专题", "政策")));
        appendBoundary(builder, validationResult.validationSummary());
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildPointAnswer(String conclusion,
                                    AiPolicyEvidenceValidator.ValidationResult validationResult,
                                    List<String> keywords) {
        StringBuilder builder = new StringBuilder();
        builder.append(conclusion).append("\n");
        appendPointBlock(builder, "当前依据", extractPoints(validationResult.primaryHits(), keywords));
        appendBoundary(builder, validationResult.validationSummary());
        appendAdvice(builder, validationResult.shouldSuggestTopicDive());
        appendCitations(builder, validationResult.primaryHits());
        return builder.toString().trim();
    }

    private String buildInsufficientAnswer(AiPolicyRegionResolver.RegionMatch regionMatch,
                                           AiPolicyIntentClassifier.IntentMatch intentMatch,
                                           AiPolicyResolver.PolicyMatch policyMatch,
                                           AiPolicyRouterService.RoutePlan routePlan,
                                           AiPolicyEvidenceValidator.ValidationResult validationResult) {
        StringBuilder builder = new StringBuilder();
        builder.append("结论：当前知识库暂未命中足够的");
        if (policyMatch != null && policyMatch.matched()) {
            builder.append(policyMatch.canonicalPolicyName() == null ? "目标政策" : policyMatch.canonicalPolicyName());
        } else if (regionMatch != null && regionMatch.scope() != AiPolicyRegionResolver.RegionScope.UNKNOWN) {
            builder.append(regionMatch.scope().getLabel()).append("政策");
        } else {
            builder.append("政策专题");
        }
        builder.append("证据，暂不能把未命中的细节当作已确认事实。\n");
        builder.append("当前依据：");
        builder.append(resolveIntentHint(intentMatch.intentType(), routePlan.detailIntent())).append("\n");
        builder.append("当前边界：").append(validationResult.validationSummary()).append("\n");
        builder.append("是否建议继续命中专题/原文：建议继续命中对应专题文档、年度申报公告或原始条款，再确认细项内容。\n");
        return builder.toString().trim();
    }

    private String resolveIntentHint(IntentType intentType, boolean detailIntent) {
        return switch (intentType) {
            case LIST -> "已识别为清单型问题，建议优先补充主文档总览或目录型专题。";
            case PROCESS -> "已识别为流程型问题，建议优先补充流程专题、年度通知或申报步骤条款。";
            case CONDITION -> "已识别为条件型问题，建议优先补充适用对象、资格要求、认定条件专题。";
            case BENEFIT -> "已识别为待遇型问题，建议优先补充补助标准、拨付安排、兑现条款。";
            case SERVICE -> "已识别为服务保障型问题，建议优先补充服务保障专题。";
            case RISK -> "已识别为管理期/风险型问题，建议优先补充管理期、退出或追回条款。";
            case COMPARE -> "已识别为对比型问题，建议分别补充厦门与福建两个地域的对应专题。";
            default -> detailIntent ? "已识别为细节型专题问题，建议优先补充对应专题文档。" : "已识别为专题型问题，建议补充该政策的专题证据。";
        };
    }

    private void appendPolicyList(StringBuilder builder, List<KnowledgeSearchResultVO> hits) {
        LinkedHashSet<String> policies = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            String policyName = firstNonBlank(hit.getPolicyName(), hit.getDocTitle());
            if (policyName != null) {
                policies.add(policyName);
            }
            if (policies.size() >= 8) {
                break;
            }
        }
        if (policies.isEmpty()) {
            builder.append("1. 当前尚未抽取出足够稳定的目录项。\n");
            return;
        }
        int index = 1;
        for (String policy : policies) {
            builder.append(index++).append(". ").append(policy).append("\n");
        }
    }

    private void appendRegionBlock(StringBuilder builder, String label, List<KnowledgeSearchResultVO> hits) {
        builder.append(label).append("：\n");
        if (hits == null || hits.isEmpty()) {
            builder.append("1. 当前未命中足够的该地域专题证据。\n");
            return;
        }
        appendPolicyList(builder, hits);
    }

    private void appendPointBlock(StringBuilder builder, String title, List<String> points) {
        builder.append(title).append("：\n");
        if (points.isEmpty()) {
            builder.append("1. 当前仅命中到部分专题线索，仍缺少可直接展开的细项条款。\n");
            return;
        }
        for (int i = 0; i < points.size(); i++) {
            builder.append(i + 1).append(". ").append(points.get(i)).append("\n");
        }
    }

    private void appendBoundary(StringBuilder builder, String boundary) {
        builder.append("当前边界：").append(boundary).append("\n");
    }

    private void appendAdvice(StringBuilder builder, boolean suggestTopicDive) {
        builder.append("是否建议继续命中专题/原文：");
        if (suggestTopicDive) {
            builder.append("建议继续命中对应专题或原始条款，补齐流程细项、材料清单、金额标准或年度要求。");
        } else {
            builder.append("当前可先依据已命中证据作答，如需更细条款仍可继续下钻专题/原文。");
        }
        builder.append("\n");
    }

    private void appendCitations(StringBuilder builder, List<KnowledgeSearchResultVO> hits) {
        builder.append("政策依据：\n");
        LinkedHashSet<String> citations = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            citations.add(firstNonBlank(hit.getDocTitle(), "未命名文档")
                    + " / "
                    + firstNonBlank(hit.getHeadingPath(), hit.getSectionTitle(), "未标注标题"));
        }
        if (citations.isEmpty()) {
            builder.append("- 当前未形成可引用的专题证据。\n");
            return;
        }
        for (String citation : citations) {
            builder.append("- ").append(citation).append("\n");
        }
    }

    private List<String> extractProcessSteps(List<KnowledgeSearchResultVO> hits) {
        List<String> orderedKeywords = List.of("组织申报", "资格核查", "部门联审", "综合评审", "研究确认", "公示", "拨付");
        LinkedHashSet<String> steps = new LinkedHashSet<>();
        for (String keyword : orderedKeywords) {
            for (KnowledgeSearchResultVO hit : hits) {
                String text = buildText(hit);
                if (text.contains(keyword)) {
                    steps.add(keyword + "： " + abbreviate(extractAround(text, keyword), 80));
                    break;
                }
            }
        }
        if (steps.isEmpty()) {
            for (KnowledgeSearchResultVO hit : hits) {
                if (steps.size() >= 5) {
                    break;
                }
                String text = abbreviate(buildText(hit), 80);
                if (!text.isBlank()) {
                    steps.add(text);
                }
            }
        }
        return new ArrayList<>(steps);
    }

    private List<String> extractPoints(List<KnowledgeSearchResultVO> hits, List<String> keywords) {
        LinkedHashSet<String> points = new LinkedHashSet<>();
        for (KnowledgeSearchResultVO hit : hits) {
            String text = buildText(hit);
            for (String keyword : keywords) {
                if (text.contains(keyword)) {
                    points.add(abbreviate(extractAround(text, keyword), 90));
                    break;
                }
            }
            if (points.size() >= 4) {
                break;
            }
        }
        return new ArrayList<>(points);
    }

    private String extractAround(String text, String anchor) {
        if (text == null || text.isBlank()) {
            return "";
        }
        int index = anchor == null ? -1 : text.indexOf(anchor);
        if (index < 0) {
            return text.replaceAll("\\s+", " ").trim();
        }
        int start = Math.max(0, index - 12);
        int end = Math.min(text.length(), index + 72);
        return text.substring(start, end).replaceAll("\\s+", " ").trim();
    }

    private String buildText(KnowledgeSearchResultVO hit) {
        return String.join(" ",
                firstNonBlank(hit.getHeadingPath(), ""),
                firstNonBlank(hit.getPolicyName(), ""),
                firstNonBlank(hit.getSnippet(), ""));
    }

    private String abbreviate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private DepartmentFocus resolveDepartmentFocus(String questionText) {
        String text = questionText == null ? "" : questionText;
        if (containsAny(text, List.of("创业人才", "创业"))) {
            return DepartmentFocus.STARTUP_TALENT;
        }
        if (containsAny(text, List.of("创新团队", "团队", "谁审核", "由谁审核", "哪个部门审核", "审核部门"))) {
            return DepartmentFocus.INNOVATION_TEAM;
        }
        if (containsAny(text, List.of("创新个人", "创新人才"))) {
            return DepartmentFocus.INNOVATION_INDIVIDUAL;
        }
        return DepartmentFocus.GENERAL;
    }

    private String buildQuestionText(AiPolicyQuestionNormalizer.NormalizedQuestion question) {
        if (question == null) {
            return "";
        }
        return String.join(" ",
                blankToEmpty(question.original()),
                blankToEmpty(question.normalized()),
                blankToEmpty(question.compact()));
    }

    private boolean containsAny(String text, List<String> keywords) {
        if (text == null || text.isBlank() || keywords == null || keywords.isEmpty()) {
            return false;
        }
        for (String keyword : keywords) {
            if (keyword != null && !keyword.isBlank() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String blankToEmpty(String text) {
        return text == null || text.isBlank() ? "" : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private enum DepartmentFocus {
        STARTUP_TALENT,
        INNOVATION_TEAM,
        INNOVATION_INDIVIDUAL,
        GENERAL
    }
}
