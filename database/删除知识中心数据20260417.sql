BEGIN;

-- 清空知识库与技能绑定关系
TRUNCATE TABLE public.ai_skill_kb_binding RESTART IDENTITY;

-- 清空知识库用户权限
TRUNCATE TABLE public.ai_user_knowledge_permission RESTART IDENTITY;

-- 清空知识库导入任务记录
TRUNCATE TABLE public.ai_document_import_job RESTART IDENTITY;

-- 清空知识库切片数据
TRUNCATE TABLE public.ai_knowledge_chunk RESTART IDENTITY;

-- 清空知识库文档
TRUNCATE TABLE public.ai_knowledge_document RESTART IDENTITY;

-- 清空知识库分类
TRUNCATE TABLE public.ai_knowledge_category RESTART IDENTITY;

-- 清空知识库主表
TRUNCATE TABLE public.ai_knowledge_base RESTART IDENTITY;

COMMIT;
