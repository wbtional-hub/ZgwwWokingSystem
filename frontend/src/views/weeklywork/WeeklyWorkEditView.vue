<template>
  <AppPageShell :title="pageShellTitle">
    <template #actions>
      <div class="action-row">
        <van-button
          v-if="isEditorPage"
          plain
          type="primary"
          :disabled="pageBusy"
          @click="navigateToListPage({ focusId: activeRecordId || editorForm.id, tab: activeRecordTab })"
        >
          返回总览
        </van-button>
        <van-button v-if="showCreateEntry" type="primary" :disabled="pageBusy" @click="openCreateForm()">
          {{ isEditorPage ? '新建另一份周报' : '新建周报' }}
        </van-button>
      </div>
    </template>

    <section v-if="showHeroSection" class="status-hero" :class="{ 'status-hero--audit': isAuditTab }">
      <div class="status-hero__top">
        <div>
          <div class="status-hero__eyebrow">{{ currentHeroEyebrow }}</div>
          <div class="status-hero__title">{{ currentHeroTitle }}</div>
        </div>
        <van-tag round :type="statusTagType(heroStatus)">{{ heroStatusLabel }}</van-tag>
      </div>
      <div class="status-hero__meta">
        <span>当前阶段：{{ heroCurrentStageLabel }}</span>
        <span>周次：{{ heroWeekNo }}</span>
        <span v-if="isAuditTab">填报人：{{ heroReporterLabel }}</span>
      </div>
      <div class="status-hero__focus-strip">
        <div class="status-hero__focus-card status-hero__focus-card--current">
          <span class="status-hero__focus-label">当前阶段</span>
          <strong class="status-hero__focus-value">{{ heroCurrentStageLabel }}</strong>
        </div>
        <div class="status-hero__focus-card" :class="{ 'status-hero__focus-card--next': heroNextStepLabel !== '无下一步' }">
          <span class="status-hero__focus-label">下一步</span>
          <strong class="status-hero__focus-value">{{ heroNextStepLabel }}</strong>
        </div>
        <div
          class="status-hero__focus-card"
          :class="{ 'status-hero__focus-card--return': heroReturnTargetLabel !== '无退回目标' }"
        >
          <span class="status-hero__focus-label">退回目标</span>
          <strong class="status-hero__focus-value">{{ heroReturnTargetLabel }}</strong>
        </div>
      </div>
      <div v-if="showHeroReturnSummary" class="status-hero__alert">
        最近退回：{{ heroReturnSummary }}
      </div>
      <div v-if="heroNotice" class="status-hero__notice">
        {{ heroNotice }}
      </div>
      <div v-if="heroFlowNodes.length" class="approval-stepper">
        <div
          v-for="step in heroFlowNodes"
          :key="step.key"
          class="approval-step"
          :class="[`approval-step--${step.state}`]"
        >
          <div class="approval-step__dot">{{ step.order }}</div>
          <div class="approval-step__label">{{ step.label }}</div>
        </div>
      </div>
      <div class="status-hero__summary">
        <span>当前模式：{{ currentModeLabel }}</span>
        <span>任务数：{{ heroTaskCount }}</span>
        <span>当前审批节点：{{ heroCurrentStageLabel }}</span>
        <span>最近更新时间：{{ heroUpdatedAt }}</span>
      </div>
    </section>

    <section v-if="!isEditorPage && isMineTab" class="calendar-board">
      <div class="calendar-board__header">
        <div>
          <div class="panel-title">周历视图</div>
          <div class="panel-hint">按周查看本月安排，日历里只显示少量任务提示，保持清爽易读。</div>
        </div>
      </div>
      <div class="calendar-board__toolbar">
        <button type="button" class="calendar-nav-button" :disabled="pageBusy" @click="shiftCalendarMonth(-1)">上月</button>
        <div class="calendar-board__month">{{ calendarMonthLabel }}</div>
        <button type="button" class="calendar-nav-button calendar-nav-button--today" :disabled="pageBusy" @click="jumpCalendarToToday">本月</button>
      </div>
      <div class="calendar-grid">
        <div class="calendar-weekdays">
          <div v-for="weekday in CALENDAR_WEEK_LABELS" :key="weekday" class="calendar-grid__weekday">{{ weekday }}</div>
        </div>
        <div
          v-for="(week, weekIndex) in calendarWeeks"
          :key="`week-${weekIndex}`"
          class="calendar-week-row"
        >
          <button
            v-for="day in week"
            :key="day.key"
            type="button"
            class="calendar-day"
            :class="{
              'calendar-day--muted': !day.isCurrentMonth,
              'calendar-day--today': day.isToday,
              'calendar-day--selected': day.dateKey === selectedCalendarDate,
              'calendar-day--has-record': day.recordCount > 0
            }"
            :disabled="pageBusy || !day.dateKey"
            @click="selectCalendarDate(day)"
          >
            <div class="calendar-day__top">
              <span class="calendar-day__date">{{ day.dateNumber }}</span>
              <span v-if="day.recordCount" class="calendar-day__badge">{{ day.recordCount }}</span>
            </div>
            <div class="calendar-day__events">
              <span
                v-for="item in day.previewItems"
                :key="`${day.key}-${item}`"
                class="calendar-day__event"
              >
                {{ item }}
              </span>
              <span v-if="day.moreCount > 0" class="calendar-day__more">+{{ day.moreCount }}</span>
            </div>
          </button>
        </div>
      </div>
      <div class="calendar-board__footer">
        <div>
          <div class="calendar-board__focus">{{ selectedCalendarTitle }}</div>
          <div class="calendar-board__hint">{{ selectedCalendarHint }}</div>
        </div>
        <van-button v-if="showCreateEntry" type="primary" size="small" :disabled="pageBusy" @click="openCreateForm({ date: selectedCalendarDate })">
          在这一天新建
        </van-button>
      </div>
      <div v-if="selectedDateRecords.length" class="calendar-record-list">
        <button
          v-for="item in selectedDateRecords"
          :key="`calendar-record-${item.id}`"
          type="button"
          class="calendar-record-card"
          :disabled="pageBusy"
          @click="handleCalendarRecordOpen(item)"
        >
          <div class="calendar-record-card__top">
            <strong>{{ item.weekNo || '-' }}</strong>
            <van-tag size="medium" :type="statusTagType(item.status)">{{ statusLabel(item.status, item) }}</van-tag>
          </div>
          <div class="calendar-record-card__meta">创建时间：{{ formatDateTime(item.createTime) }}</div>
          <div class="calendar-record-card__meta">任务概览：{{ summarizeTask(item) }}</div>
        </button>
      </div>
    </section>

    <section v-if="isEditorPage" class="task-board">
      <div class="task-board__header">
        <div>
          <div class="panel-title">当前周报任务列表</div>
          <div class="task-board__summary">共 {{ taskList.length }} 项任务，草稿和退回状态可继续维护。</div>
        </div>
        <div class="task-board__actions">
          <van-button v-if="taskList.length" size="small" type="primary" :disabled="pageBusy || !canEditCurrentWeekly" @click="openTaskEditor()">
            {{ currentTaskActionLabel }}
          </van-button>
        </div>
      </div>

      <van-field
        v-model.trim="editorForm.weekNo"
        label="周次"
        placeholder="如：2026-W12"
        :disabled="pageBusy || !canEditCurrentWeekly"
      />

      <div v-if="taskList.length" class="task-list">
        <article
          v-for="(task, index) in taskList"
          :key="`task-${index}`"
          class="task-card"
          :class="{ 'task-card--editable': canEditCurrentWeekly }"
          @click="handleTaskCardClick(index)"
        >
          <div class="task-card__header">
            <div>
              <div class="task-card__title">任务 {{ index + 1 }}</div>
              <div class="task-card__subtitle">{{ task.receiveTime || '待补领取时间' }} 开始跟进</div>
            </div>
            <div class="task-pill">{{ taskNatureLabel(task.nature) }}</div>
          </div>
          <div class="task-card__grid">
            <div class="task-kv">
              <span class="task-kv__label">领取任务时间</span>
              <span class="task-kv__value">{{ task.receiveTime || '待填写' }}</span>
            </div>
            <div class="task-kv">
              <span class="task-kv__label">完成时限</span>
              <span class="task-kv__value">{{ task.deadline || '待填写' }}</span>
            </div>
          </div>
          <div class="task-card__block">
            <div class="task-card__label">目前进展</div>
            <div class="task-card__content">{{ task.progress || '暂未填写进展说明' }}</div>
          </div>
          <div class="task-card__block">
            <div class="task-card__label">需要协助</div>
            <div class="task-card__content">{{ task.assistance || '暂未填写协助需求' }}</div>
          </div>
          <div v-if="canEditCurrentWeekly" class="task-card__footer">
            <span class="task-card__hint">点击卡片可编辑</span>
            <button type="button" class="task-card__delete" :disabled="pageBusy" @click.stop="handleDeleteTask(index)">删除</button>
          </div>
        </article>
      </div>
      <div v-else class="task-empty-card">
        <div class="task-empty-card__title">当前周报还没有任务</div>
        <div class="task-empty-card__desc">先新增本周任务，再暂存或提交审核。</div>
        <van-button size="small" type="primary" :disabled="pageBusy || !canEditCurrentWeekly" @click="openTaskEditor()">
          新增第一项任务
        </van-button>
      </div>

      <div v-if="showEditorActionPanel" class="editor-action-panel">
        <div class="editor-action-panel__info">
          <div class="editor-action-panel__title">当前周报操作</div>
          <div class="editor-action-panel__desc">{{ stickyActionHint }}</div>
        </div>
        <div class="editor-action-panel__buttons">
          <van-button
            v-if="showSaveButton"
            plain
            type="primary"
            :loading="submitting"
            :disabled="pageBusy"
            @click="handleSaveDraft"
          >
            保存草稿
          </van-button>
          <van-button
            v-if="showSubmitButton"
            type="success"
            :loading="submitting"
            :disabled="pageBusy || !editorForm.weekNo"
            @click="handleSubmitCurrent"
          >
            {{ submitActionLabel }}
          </van-button>
        </div>
      </div>
    </section>

    <section v-else-if="showAuditSummarySection" class="audit-mode-board">
      <div class="audit-mode-board__header">
        <div>
          <div class="panel-title">{{ activeRecordTab === 'pending' ? '审核说明区' : '审核回看区' }}</div>
          <div class="panel-hint">{{ auditModeHint }}</div>
        </div>
      </div>
      <div v-if="showAuditModePlaceholder" class="audit-mode-card audit-mode-card--placeholder">
        <div class="audit-mode-card__top">
          <div>
            <div class="audit-mode-card__eyebrow">{{ activeRecordTab === 'pending' ? '待审焦点切换中' : '已审回看切换中' }}</div>
            <div class="audit-mode-card__title">{{ auditPlaceholderTitle }}</div>
          </div>
          <van-tag type="primary">切换中</van-tag>
        </div>
        <div class="audit-mode-card__grid">
          <div class="audit-mode-kv" v-for="field in AUDIT_PLACEHOLDER_FIELDS" :key="field.key">
            <span class="audit-mode-kv__label">{{ field.label }}</span>
            <span class="audit-mode-kv__value">{{ field.value }}</span>
          </div>
        </div>
        <div class="audit-mode-card__hint">{{ auditPlaceholderDesc }}</div>
      </div>
      <div v-else-if="auditFocusRecord" class="audit-mode-card">
        <div class="audit-mode-card__top">
          <div>
            <div class="audit-mode-card__eyebrow">{{ activeRecordTab === 'pending' ? '当前选中待审记录' : '当前选中已审记录' }}</div>
            <div class="audit-mode-card__title">{{ recordCardTitle(auditFocusRecord) }}</div>
          </div>
          <van-tag :type="statusTagType(auditFocusRecord.status)">{{ statusLabel(auditFocusRecord.status, auditFocusRecord) }}</van-tag>
        </div>
        <div class="audit-mode-card__grid">
          <div class="audit-mode-kv">
            <span class="audit-mode-kv__label">当前阶段</span>
            <span class="audit-mode-kv__value">{{ currentStageText(auditFocusRecord) }}</span>
          </div>
          <div class="audit-mode-kv">
            <span class="audit-mode-kv__label">下一步</span>
            <span class="audit-mode-kv__value">{{ nextFlowStepText(auditFocusRecord) }}</span>
          </div>
          <div class="audit-mode-kv">
            <span class="audit-mode-kv__label">退回目标</span>
            <span class="audit-mode-kv__value">{{ returnTargetText(auditFocusRecord) }}</span>
          </div>
          <div class="audit-mode-kv">
            <span class="audit-mode-kv__label">最近更新时间</span>
            <span class="audit-mode-kv__value">{{ formatDateTime(resolveRecordUpdatedAt(auditFocusRecord)) }}</span>
          </div>
        </div>
        <div class="audit-mode-card__hint">
          {{ activeRecordTab === 'pending' ? '顶部已切换为审核摘要视角，继续通过详情页和审批操作区完成处理。' : '顶部保持审核回看视角，可连续查看已处理记录而不会误入编辑态。' }}
        </div>
      </div>
      <div v-else class="audit-mode-empty">
        <div class="audit-mode-empty__title">{{ activeRecordTab === 'pending' ? '暂无待审核记录' : '暂无已审核记录' }}</div>
        <div class="audit-mode-empty__desc">顶部已切换为审核模式，不再默认呈现填报编辑区。</div>
      </div>
    </section>

    <section v-if="!isEditorPage" class="record-board">
      <div class="record-board__header">
        <div>
          <div class="panel-title">周报记录区</div>
          <div class="panel-hint">按填报与审核角色拆分入口，避免同一分区里混用填报和审核动作。</div>
        </div>
      </div>

      <div class="record-tabs">
        <button
          v-for="tab in recordTabs"
          :key="tab.key"
          type="button"
          class="record-tab"
          :class="{ 'record-tab--active': activeRecordTab === tab.key }"
          :disabled="pageBusy"
          @click="handleRecordTabChange(tab.key)"
        >
          <span>{{ tab.label }}</span>
          <span class="record-tab__count">{{ tab.count }}</span>
        </button>
      </div>

      <div
        class="record-focus-banner"
        :class="{
          'record-focus-banner--audit': isAuditTab,
          'record-focus-banner--transition': showTransitionPlaceholder
        }"
      >
        <div class="record-focus-banner__title">{{ currentViewBannerTitle }}</div>
        <div class="record-focus-banner__desc">{{ currentViewBannerDesc }}</div>
      </div>

      <van-loading v-if="listLoading" size="24px" vertical class="state-block">加载中...</van-loading>

      <van-empty v-else-if="!currentTabRecords.length" :description="currentRecordTab.emptyTitle">
        <template #default>
          <div class="empty-tip">{{ currentRecordTab.emptyDesc }}</div>
        </template>
      </van-empty>

      <div v-else class="record-board__content">
        <div class="record-group">
          <div class="record-group__title">{{ currentRecordTab.title }}</div>
          <div class="panel-hint">{{ currentRecordTab.desc }}</div>
          <div class="flow-state-legend">
            <span
              v-for="legend in FLOW_STATE_LEGEND"
              :key="legend.key"
              class="flow-state-legend__item"
              :class="`flow-state-legend__item--${legend.key}`"
            >
              {{ legend.label }}
            </span>
          </div>
          <div class="list-wrap">
            <div
              v-for="item in currentTabRecords"
              :key="`${activeRecordTab}-${item.id}`"
              class="report-card-wrap"
              :class="{ 'report-card-wrap--active': isActiveRecord(item) }"
              @click="handleRecordCardClick(item)"
            >
            <van-card class="report-card">
              <template #title>
                <div class="report-title">
                  <div class="report-title__main">
                    <span>{{ recordCardTitle(item) }}</span>
                    <span v-if="isActiveRecord(item)" class="report-active-badge">{{ activeRecordBadgeLabel }}</span>
                  </div>
                  <van-tag :type="statusTagType(item.status)">{{ statusLabel(item.status, item) }}</van-tag>
                </div>
              </template>
              <template #desc>
                <div class="report-meta">任务数：{{ recordTaskCount(item) }}</div>
                <div class="report-stage-strip">
                  <div class="report-stage-chip report-stage-chip--current">
                    <span class="report-stage-chip__label">当前阶段</span>
                    <strong class="report-stage-chip__value">{{ currentStageText(item) }}</strong>
                  </div>
                  <div class="report-stage-chip" :class="{ 'report-stage-chip--next': nextFlowStepText(item) !== '无下一步' }">
                    <span class="report-stage-chip__label">下一步</span>
                    <strong class="report-stage-chip__value">{{ nextFlowStepText(item) }}</strong>
                  </div>
                  <div class="report-stage-chip" :class="{ 'report-stage-chip--return': returnTargetText(item) !== '无退回目标' }">
                    <span class="report-stage-chip__label">退回目标</span>
                    <strong class="report-stage-chip__value">{{ returnTargetText(item) }}</strong>
                  </div>
                </div>
                <div class="report-meta">当前状态：{{ statusLabel(item.status, item) }}</div>
                <div class="report-meta">当前流转目标：{{ currentFlowTargetText(item) }}</div>
                <div class="report-flow-stepper">
                  <div
                    v-for="step in buildFlowNodes(item)"
                    :key="`card-${item.id}-${step.key}`"
                    class="approval-step approval-step--compact"
                    :class="[`approval-step--${step.state}`]"
                  >
                    <div class="approval-step__dot">{{ step.order }}</div>
                    <div class="approval-step__label">{{ step.label }}</div>
                    <div class="approval-step__state">{{ flowNodeStateLabel(step, item) }}</div>
                  </div>
                </div>
                <div class="report-meta">最近更新时间：{{ formatDateTime(resolveRecordUpdatedAt(item)) }}</div>
                <div v-if="shouldShowReturnSummary(item) && recordReturnSummary(item)" class="report-meta report-meta--return">最近退回：{{ recordReturnSummary(item) }}</div>
                <div class="report-preview task-text-block">任务概览：{{ summarizeTask(item) }}</div>
              </template>
              <template #footer>
                <div class="card-actions">
                  <van-button
                    v-if="showCardDetailAction(item)"
                    size="small"
                    plain
                    type="primary"
                    :disabled="pageBusy"
                    @click.stop="openDetail(item, { syncCurrent: activeRecordTab === 'mine' })"
                  >
                    查看详情
                  </van-button>
                  <van-button
                    v-if="showCardEditAction(item)"
                    size="small"
                    plain
                    type="primary"
                    :disabled="pageBusy"
                    @click.stop="openEditForm(item)"
                  >
                    继续编辑
                  </van-button>
                  <van-button
                    v-if="showCardSubmitAction(item)"
                    size="small"
                    plain
                    type="primary"
                    :loading="submitTargetId === item.id"
                    :disabled="pageBusy || !canSubmit(item)"
                    @click.stop="handleSubmitFromList(item)"
                  >
                    提交到下一步
                  </van-button>
                  <van-button
                    v-if="showCardReviewAction(item)"
                    size="small"
                    type="primary"
                    :disabled="pageBusy || !canReview(item)"
                    @click.stop="openDetail(item, { syncCurrent: false })"
                  >
                    立即审核
                  </van-button>
                </div>
              </template>
            </van-card>
            </div>
          </div>
        </div>
      </div>
    </section>

    <van-popup
      v-model:show="detailVisible"
      position="bottom"
      round
      :close-on-click-overlay="!detailLoading"
      @update:show="handleDetailVisibilityChange"
    >
      <div class="detail-panel">
        <div class="detail-header">
          <div class="detail-title">周报详情</div>
          <van-button plain size="small" :disabled="detailLoading" @click="handleExitView">关闭</van-button>
        </div>
        <div v-if="detailLoading" class="detail-loading-card">
          <van-loading size="24px" vertical class="state-block">{{ detailLoadingLabel }}</van-loading>
          <div class="detail-loading-card__desc">{{ detailLoadingDesc }}</div>
        </div>
        <template v-else-if="detailRecord">
          <div class="detail-summary-card">
            <div class="detail-summary-card__top">
              <div>
                <div class="detail-summary-card__eyebrow">周次</div>
                <div class="detail-summary-card__title">{{ detailRecord.weekNo || '-' }}</div>
              </div>
              <van-tag round :type="statusTagType(detailRecord.status)">{{ statusLabel(detailRecord.status, detailRecord) }}</van-tag>
            </div>
            <div class="detail-summary-grid">
              <div class="detail-summary-kv">
                <span class="detail-summary-kv__label">任务数</span>
                <span class="detail-summary-kv__value">{{ recordTaskCount(detailRecord) }}</span>
              </div>
              <div class="detail-summary-kv">
                <span class="detail-summary-kv__label">当前阶段</span>
                <span class="detail-summary-kv__value">{{ currentStageText(detailRecord) }}</span>
              </div>
              <div class="detail-summary-kv">
                <span class="detail-summary-kv__label">下一步</span>
                <span class="detail-summary-kv__value">{{ nextFlowStepText(detailRecord) }}</span>
              </div>
              <div class="detail-summary-kv">
                <span class="detail-summary-kv__label">退回目标</span>
                <span class="detail-summary-kv__value">{{ returnTargetText(detailRecord) }}</span>
              </div>
              <div class="detail-summary-kv">
                <span class="detail-summary-kv__label">填报人</span>
                <span class="detail-summary-kv__value">{{ detailRecord.realName || detailRecord.username || '-' }}</span>
              </div>
              <div class="detail-summary-kv">
                <span class="detail-summary-kv__label">最近更新时间</span>
                <span class="detail-summary-kv__value">{{ formatDateTime(resolveRecordUpdatedAt(detailRecord)) }}</span>
              </div>
            </div>
            <div class="detail-summary-card__chain">真实审批链：{{ flowChainSummary(detailRecord) }}</div>
            <div class="detail-summary-card__chain">节点状态：{{ flowStateSummary(detailRecord) }}</div>
            <div v-if="shouldShowReturnSummary(detailRecord) && recordReturnSummary(detailRecord)" class="detail-summary-card__return">
              最近退回：{{ recordReturnSummary(detailRecord) }}
            </div>
            <div class="flow-state-legend flow-state-legend--detail">
              <span
                v-for="legend in FLOW_STATE_LEGEND"
                :key="`detail-${legend.key}`"
                class="flow-state-legend__item"
                :class="`flow-state-legend__item--${legend.key}`"
              >
                {{ legend.label }}
              </span>
            </div>
            <div class="detail-flow-stepper">
              <div
                v-for="step in detailFlowNodes"
                :key="`detail-${step.key}`"
                class="approval-step"
                :class="[`approval-step--${step.state}`]"
              >
                <div class="approval-step__dot">{{ step.order }}</div>
                <div class="approval-step__label">{{ step.label }}</div>
                <div class="approval-step__state">{{ flowNodeStateLabel(step, detailRecord) }}</div>
              </div>
            </div>
          </div>
          <div class="detail-block">
            <div class="detail-label">任务列表</div>
            <div v-if="detailTasks.length" class="detail-task-list">
              <div v-for="(task, index) in detailTasks" :key="`detail-task-${index}`" class="detail-task-card">
                <div class="detail-task-card__head">
                  <span>任务 {{ index + 1 }}</span>
                  <van-tag plain type="primary">{{ taskNatureLabel(task.nature) }}</van-tag>
                </div>
                <div class="detail-task-card__meta task-text-block">领取任务时间：{{ task.receiveTime || '未填写' }}</div>
                <div class="detail-task-card__meta task-text-block">完成时限：{{ task.deadline || '未填写' }}</div>
                <div class="detail-task-card__block task-text-block">目前进展：{{ task.progress || '未填写' }}</div>
                <div class="detail-task-card__block task-text-block">需要协助：{{ task.assistance || '无' }}</div>
              </div>
            </div>
            <div v-else class="detail-value">未填写任务</div>
          </div>
          <div v-if="shouldShowReturnSummary(detailRecord) && recordReturnSummary(detailRecord)" class="detail-block">
            <div class="detail-label">最近退回</div>
            <div class="detail-value">
              {{ recordReturnSummary(detailRecord) }}
            </div>
          </div>
          <div v-if="approvalTimelineEntries.length" class="detail-block approval-timeline">
            <div class="detail-label">审批记录</div>
            <div v-for="log in approvalTimelineEntries" :key="log.id" class="timeline-item">
              <div class="timeline-item__head">
                <span class="timeline-tag" :class="`timeline-tag--${String(log.action || '').toLowerCase()}`">
                  {{ log.actionLabel }}
                </span>
                <span class="timeline-item__time">{{ formatDateTime(log.createTime) }}</span>
              </div>
              <div class="timeline-title">{{ log.title }}</div>
              <div class="timeline-detail">处理人：{{ log.reviewerName || '-' }}</div>
              <div class="timeline-detail">审批意见：{{ log.comment || '无审批意见' }}</div>
              <div v-if="log.toNode && log.action === 'RETURN'" class="timeline-detail timeline-detail--return">
                退回目标：{{ approvalNodeLabel(log.toNode, detailRecord) }}
              </div>
              <div v-else-if="log.toNode && log.action !== 'SUBMIT'" class="timeline-detail">
                当前流转至：{{ approvalTargetLabel(log, detailRecord) }}
              </div>
            </div>
          </div>
          <div v-if="showApprovalActions(detailRecord)" class="detail-block approval-action-card">
            <div class="detail-label">审批操作区</div>
            <div class="approval-action-card__hint">
              当前节点：{{ approvalNodeLabel(resolveCurrentApprovalNode(detailRecord), detailRecord) }}
            </div>
            <div class="approval-action-grid">
              <van-button
                v-if="canReview(detailRecord)"
                type="primary"
                size="large"
                :loading="reviewTargetId === detailRecord.id && reviewAction === 'APPROVE'"
                :disabled="pageBusy"
                @click="handleApprove(detailRecord)"
              >
                {{ reviewApproveButtonLabel(detailRecord) }}
              </van-button>
              <van-button
                v-if="canReview(detailRecord)"
                plain
                type="danger"
                size="large"
                class="approval-action-grid__danger"
                :loading="reviewTargetId === detailRecord.id && reviewAction === 'RETURN'"
                :disabled="pageBusy"
                @click="openReturnSheet(detailRecord)"
              >
                退回上一级
              </van-button>
            </div>
          </div>
        </template>
        <div v-else class="detail-empty-card">
          <div class="detail-empty-card__title">当前没有可查看的周报详情</div>
          <div class="detail-empty-card__desc">
            {{ isAuditTab ? '当前分区暂无聚焦记录，切换其他记录或等待新的审批数据。' : '请先从我的填报中选择一条记录，或先新建周报。' }}
          </div>
        </div>
      </div>
    </van-popup>

    <van-popup
      v-model:show="returnSheetVisible"
      position="bottom"
      round
      :close-on-click-overlay="!pageBusy"
      class="return-sheet-popup"
    >
      <div class="return-sheet">
        <div class="return-sheet__title">退回上一级</div>
        <div class="return-sheet__subtitle">
          当前审批节点：{{ approvalNodeLabel(resolveCurrentApprovalNode(returnSheetRecord), returnSheetRecord) }}
        </div>
        <div class="return-sheet__section">
          <div class="return-sheet__label">选择退回目标</div>
          <div class="return-target-list">
            <button
              v-for="target in returnSheetTargets"
              :key="target"
              type="button"
              class="return-target-card"
              :class="{ 'return-target-card--active': returnSheetDraft.target === target }"
              :disabled="pageBusy"
              @click="returnSheetDraft.target = target"
            >
              <span class="return-target-card__title">{{ approvalNodeLabel(target, returnSheetRecord) }}</span>
              <span class="return-target-card__desc">退回后由该节点继续处理</span>
            </button>
          </div>
        </div>
        <van-field
          v-model="returnSheetDraft.comment"
          label="退回意见"
          type="textarea"
          rows="4"
          autosize
          maxlength="500"
          show-word-limit
          placeholder="请输入明确的退回意见"
          :disabled="pageBusy"
        />
        <div class="return-sheet__actions">
          <van-button plain size="large" :disabled="pageBusy" @click="closeReturnSheet">取消</van-button>
          <van-button type="danger" size="large" :disabled="pageBusy" @click="confirmReturnFromSheet">确认退回</van-button>
        </div>
      </div>
    </van-popup>

    <van-popup
      v-model:show="taskEditorVisible"
      position="bottom"
      round
      :close-on-click-overlay="!pageBusy"
      class="task-editor-popup"
    >
      <div class="task-editor-sheet">
        <div class="task-editor-sheet__title">编辑任务卡</div>
        <div class="task-editor-sheet__section">
          <div class="task-editor-sheet__label">任务性质</div>
          <div class="nature-pill-group">
            <button
              v-for="option in taskNatureOptions"
              :key="option.value"
              type="button"
              class="nature-pill"
              :class="{ 'nature-pill--active': taskDraft.nature === option.value }"
              :disabled="pageBusy"
              @click="taskDraft.nature = option.value"
            >
              {{ option.label }}
            </button>
          </div>
        </div>
        <div class="task-editor-sheet__grid">
          <van-field v-model="taskDraft.receiveTime" label="领取时间" type="date" :disabled="pageBusy" />
          <van-field v-model="taskDraft.deadline" label="完成时限" type="date" :disabled="pageBusy" />
        </div>
        <van-field
          v-model="taskDraft.progress"
          label="目前进展"
          type="textarea"
          rows="4"
          autosize
          placeholder="输入本周任务推进情况"
          :disabled="pageBusy"
        />
        <van-field
          v-model="taskDraft.assistance"
          label="需要协助"
          type="textarea"
          rows="3"
          autosize
          placeholder="输入需要协调支持的内容"
          :disabled="pageBusy"
        />
        <div class="task-editor-sheet__actions">
          <van-button plain :disabled="pageBusy" @click="closeTaskEditor">取消</van-button>
          <van-button type="primary" :disabled="pageBusy" @click="applyTaskDraft">完成编辑</van-button>
        </div>
      </div>
    </van-popup>

  </AppPageShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import { useRoute, useRouter } from 'vue-router'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryOrgAncestors, queryOrgChildren } from '@/api/orgtree'
import { useUserStore } from '@/stores/user'
import {
  queryWeeklyWorkDetail,
  queryWeeklyWorkList,
  reviewWeeklyWork,
  saveWeeklyWorkDraft,
  submitWeeklyWork
} from '@/api/weeklywork'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const TASKS_MARKER = 'TASKS_JSON::'
const DEV_FOCUS_DEBUG = import.meta.env.DEV
const CALENDAR_WEEK_LABELS = ['一', '二', '三', '四', '五', '六', '日']

const editorForm = reactive(createEmptyEditor())
const editorMode = ref('create')
const list = ref([])
const listLoading = ref(false)
const submitting = ref(false)
const detailLoading = ref(false)
const detailVisible = ref(false)
const detailRecord = ref(null)
const activeRecordId = ref(null)
const activeRecordTab = ref('mine')
const focusTransitioning = ref(false)
const focusTransitionLabel = ref('')
const submitTargetId = ref(null)
const reviewTargetId = ref(null)
const reviewAction = ref('')
const taskEditorVisible = ref(false)
const returnSheetVisible = ref(false)
const returnSheetRecord = ref(null)
const editorMeta = reactive(createDefaultEditorMeta())
const taskList = ref([])
const taskDraft = reactive(createEmptyTaskDraft())
const editingTaskIndex = ref(-1)
const returnSheetDraft = reactive({
  target: '',
  comment: ''
})
const approvalRevisionMode = ref(false)
const editorSnapshot = ref('')
const calendarCursor = ref(getMonthStart())
const selectedCalendarDate = ref(formatDateKey(new Date()))
const orgChildCount = ref(null)
const orgChainMap = reactive({})
const orgChainRequestMap = new Map()
const LEGACY_FLOW_SEQUENCE = ['STAFF', 'SECTION_CHIEF', 'DEPUTY_LEADER', 'LEGION_LEADER']
const FLOW_STATE_LEGEND = [
  { key: 'done', label: '已处理' },
  { key: 'current', label: '当前阶段' },
  { key: 'pending', label: '待处理' },
  { key: 'returned', label: '已退回' },
  { key: 'completed', label: '已完成' }
]
const AUDIT_PLACEHOLDER_FIELDS = [
  { key: 'current', label: '当前阶段', value: '正在切换...' },
  { key: 'next', label: '下一步', value: '正在刷新...' },
  { key: 'return', label: '退回目标', value: '正在判断...' },
  { key: 'updated', label: '最近更新时间', value: '请稍候' }
]
const LEGACY_FLOW_NODE_META = {
  STAFF: { label: '科员', order: 1, roleCode: 'STAFF' },
  SECTION_CHIEF: { label: '科长（主任）', order: 2, roleCode: 'SECTION_CHIEF' },
  DEPUTY_LEADER: { label: '分管领导', order: 3, roleCode: 'DEPUTY_LEADER' },
  LEGION_LEADER: { label: '团长', order: 4, roleCode: 'LEGION_LEADER' }
}
const REAL_POST_LABEL_FIELDS = [
  'postName',
  'positionName',
  'orgRoleName',
  'jobTitle',
  'jobName',
  'positionLabel',
  'postLabel',
  'orgPostName',
  'orgRoleLabel',
  'dutyName',
  'stationName',
  'title'
]
const DISPLAY_LABEL_FIELDS = [
  'label',
  'displayLabel',
  'displayName',
  'nodeLabel',
  'name',
  'roleName',
  'roleLabel'
]
const PERSON_LABEL_FIELDS = [
  'realName',
  'reviewerName',
  'username',
  'userName',
  'approverName',
  'ownerName',
  'creatorName',
  'submitterName'
]
const LEGACY_GENERIC_LABEL_SET = new Set(
  Object.values(LEGACY_FLOW_NODE_META).map((item) => item.label)
)

const taskNatureOptions = [
  { value: 'UPPER', label: '上级' },
  { value: 'GROUP', label: '本团' },
  { value: 'SECTION', label: '本科（室）' },
  { value: 'SELF', label: '自行' }
]

const isEditorPage = computed(() => route.path === '/weekly-work/editor')
const routeRecordId = computed(() => Number(route.query.id || 0) || null)
const routeDraftDate = computed(() => normalizeDateInput(route.query.date))
const pageShellTitle = computed(() => isEditorPage.value ? '周报编辑' : '周报总览')
const showCreateEntry = computed(() => false)
const showHeroSection = computed(() => false)
const showAuditSummarySection = computed(() => false)
const isLowestLevelUser = computed(() => Number(orgChildCount.value || 0) === 0)
const showAuditRecordTabs = computed(() => !isLowestLevelUser.value)
const currentUserId = computed(() => Number(userStore.userInfo?.userId || 0))
const isMineTab = computed(() => activeRecordTab.value === 'mine')
const isAuditTab = computed(() => !isMineTab.value)
const pageBusy = computed(() => {
  return listLoading.value || submitting.value || detailLoading.value || reviewTargetId.value !== null || submitTargetId.value !== null
})
const currentWeeklyStatus = computed(() => editorMeta.status || 'DRAFT')
const currentApprovalNode = computed(() => editorMeta.currentApprovalNode || 'STAFF')
const currentStatusLabel = computed(() => statusLabel(
  currentWeeklyStatus.value,
  currentSourceRecord.value || { status: currentWeeklyStatus.value, currentApprovalNode: currentApprovalNode.value }
))
const currentApprovalNodeLabel = computed(() => approvalNodeLabel(
  currentApprovalNode.value,
  currentSourceRecord.value || { currentApprovalNode: currentApprovalNode.value }
))
const currentWeeklyTitle = computed(() => `${editorForm.weekNo || '本周'}周报`)
const currentWeeklyTaskCount = computed(() => taskList.value.length)
const currentWeeklyUpdatedAt = computed(() => formatDateTime(resolveRecordUpdatedAt(currentSourceRecord.value || {
  ...editorForm,
  ...editorMeta
})))
const currentMode = computed(() => {
  if (isAuditTab.value) {
    return activeRecordTab.value === 'pending' && canReview(auditFocusRecord.value || {}) ? 'review' : 'view'
  }
  if (approvalRevisionMode.value) {
    return 'approval_edit'
  }
  if (!activeRecordId.value) {
    return 'create'
  }
  if (canEditCurrentWeekly.value) {
    return 'edit'
  }
  if (canReview(currentSourceRecord.value || {})) {
    return 'review'
  }
  return 'view'
})
const currentModeLabel = computed(() => ({
  create: '新建本周周报',
  edit: '编辑周报',
  view: '查看周报',
  review: '审批处理中',
  approval_edit: '审批修改模式'
}[currentMode.value] || '查看周报'))
const recordsByCreateDate = computed(() => {
  return myWeeklyRecords.value.reduce((accumulator, item) => {
    const dateKey = resolveRecordCreateDate(item)
    if (!dateKey) {
      return accumulator
    }
    if (!accumulator[dateKey]) {
      accumulator[dateKey] = []
    }
    accumulator[dateKey].push(item)
    return accumulator
  }, {})
})
const calendarMonthLabel = computed(() => {
  const year = calendarCursor.value.getFullYear()
  const month = String(calendarCursor.value.getMonth() + 1).padStart(2, '0')
  return `${year}年${month}月`
})
const calendarDays = computed(() => buildCalendarDays(calendarCursor.value, recordsByCreateDate.value, selectedCalendarDate.value))
const calendarWeeks = computed(() => chunkCalendarWeeks(calendarDays.value))
const selectedDateRecords = computed(() => {
  return [...(recordsByCreateDate.value[selectedCalendarDate.value] || [])]
    .sort((left, right) => String(right.createTime || '').localeCompare(String(left.createTime || '')))
})
const selectedCalendarTitle = computed(() => `${selectedCalendarDate.value || '-'} 新增情况`)
const selectedCalendarHint = computed(() => {
  return selectedDateRecords.value.length
    ? `当天共新增 ${selectedDateRecords.value.length} 份周报，可点击记录继续查看或编辑。`
    : '当天还没有新增记录，可以直接从这里发起新建。'
})
const currentModeEyebrow = computed(() => {
  if (currentMode.value === 'create') {
    return '当前周报'
  }
  return `${currentModeLabel.value}${editorForm.id ? ` #${editorForm.id}` : ''}`
})
const currentModeTitle = computed(() => {
  if (isAuditTab.value) {
    return '审核模式'
  }
  if (currentMode.value === 'create') {
    return `新建 ${editorForm.weekNo || getCurrentWeekNo()} 周报`
  }
  return `${currentWeeklyTitle.value} · ${currentModeLabel.value}`
})
const currentModeNotice = computed(() => {
  if (currentMode.value === 'approval_edit') {
    return '当前处于审批修改模式，完成修改后可直接点击底部“提交到下一步”。'
  }
  if (currentMode.value === 'review') {
    return '当前记录待你处理，请通过详情中的审批操作区完成审核。'
  }
  return ''
})
const auditModeHint = computed(() => {
  if (activeRecordTab.value === 'pending') {
    return '顶部只展示审核说明与当前选中记录摘要，避免产生“正在编辑他人周报”的错觉。'
  }
  return '顶部保持已审核记录摘要与流程回看提示，可连续查看多条记录而不进入填报编辑态。'
})
const currentTaskActionLabel = computed(() => '新增任务')
const showEditorActionPanel = computed(() => isEditorPage.value && isMineTab.value && ['create', 'edit', 'approval_edit'].includes(currentMode.value))
const showSaveButton = computed(() => ['create', 'edit', 'approval_edit'].includes(currentMode.value))
const showSubmitButton = computed(() => ['create', 'edit', 'approval_edit'].includes(currentMode.value))
const submitActionLabel = computed(() => '提交到下一步')
const stickyActionHint = computed(() => `${statusLabel(currentWeeklyStatus.value, currentSourceRecord.value || { status: currentWeeklyStatus.value, currentApprovalNode: currentApprovalNode.value })} · ${currentApprovalNodeLabel.value}`)
const shortReturnComment = computed(() => {
  const value = editorMeta.lastReturnComment || ''
  return value.length > 28 ? `${value.slice(0, 28)}...` : value
})
const canEditCurrentWeekly = computed(() => {
  return !pageBusy.value && (
    editorMeta.status === 'DRAFT' ||
    (editorMeta.status === 'RETURNED' && matchesFlowNode(currentFlowNodes.value[0], currentApprovalNode.value)) ||
    approvalRevisionMode.value
  )
})
const myWeeklyRecords = computed(() => {
  return list.value.filter((item) => Number(item.userId) === currentUserId.value)
})
const pendingReviewRecords = computed(() => {
  return list.value.filter((item) => canReview(item))
})
const processedReviewRecords = computed(() => {
  return list.value.filter((item) => Number(item.userId) !== currentUserId.value && Boolean(item.reviewedByCurrentUser) && !canReview(item))
})
const recordTabs = computed(() => ([
  {
    key: 'mine',
    label: '我的填报',
    title: '我的填报',
    desc: '这里只保留填报动作，只看我自己提交的周报。',
    emptyTitle: '暂无我的填报',
    emptyDesc: '先新建并暂存或提交周报，填报记录会集中显示在这里。',
    count: myWeeklyRecords.value.length
  },
  ...(showAuditRecordTabs.value ? [
    {
      key: 'pending',
      label: '待我审核',
      title: '待我审核',
      desc: '这里只保留审核入口和流程阶段，避免混入编辑与填报动作。',
      emptyTitle: '暂无待我审核',
      emptyDesc: '当前没有需要你处理的周报。',
      count: pendingReviewRecords.value.length
    },
    {
      key: 'processed',
      label: '我已审核',
      title: '我已审核',
      desc: '这里展示我已处理过的周报，支持回看流程和处理结果。',
      emptyTitle: '暂无我已审核',
      emptyDesc: '你处理过的周报会显示在这里。',
      count: processedReviewRecords.value.length
    }
  ] : [])
]))
const currentRecordTab = computed(() => {
  return recordTabs.value.find((tab) => tab.key === activeRecordTab.value) || recordTabs.value[0]
})
const currentTabRecords = computed(() => {
  if (activeRecordTab.value === 'pending') {
    return pendingReviewRecords.value
  }
  if (activeRecordTab.value === 'processed') {
    return processedReviewRecords.value
  }
  return myWeeklyRecords.value
})
const showTransitionPlaceholder = computed(() => {
  return focusTransitioning.value || (isAuditTab.value && detailLoading.value)
})
const showAuditModePlaceholder = computed(() => {
  return isAuditTab.value && showTransitionPlaceholder.value && !auditFocusRecord.value
})
const auditFocusRecord = computed(() => {
  if (!isAuditTab.value) {
    return null
  }
  if (showTransitionPlaceholder.value) {
    return null
  }
  if (detailVisible.value && detailRecord.value && currentTabRecords.value.some((item) => Number(item.id) === Number(detailRecord.value?.id))) {
    return detailRecord.value
  }
  if (activeRecordId.value) {
    const matched = currentTabRecords.value.find((item) => Number(item.id) === Number(activeRecordId.value))
    if (matched) {
      return matched
    }
  }
  return currentTabRecords.value[0] || null
})
const activeRecord = computed(() => {
  if (!activeRecordId.value) {
    return null
  }
  return list.value.find((item) => Number(item.id) === Number(activeRecordId.value)) || null
})
const currentSourceRecord = computed(() => {
  if (detailVisible.value && Number(detailRecord.value?.id) === Number(activeRecordId.value)) {
    return detailRecord.value
  }
  return activeRecord.value || null
})
const heroRecord = computed(() => {
  if (isMineTab.value) {
    return currentSourceRecord.value || {
      ...editorForm,
      ...editorMeta
    }
  }
  return auditFocusRecord.value
})
const heroStatus = computed(() => heroRecord.value?.status || (isMineTab.value ? currentWeeklyStatus.value : 'DRAFT'))
const heroStatusLabel = computed(() => {
  if (showTransitionPlaceholder.value) {
    return '切换中'
  }
  if (!heroRecord.value && isAuditTab.value) {
    return activeRecordTab.value === 'pending' ? '待处理审核' : '审核回看'
  }
  return statusLabel(heroStatus.value, heroRecord.value || { status: heroStatus.value })
})
const heroWeekNo = computed(() => heroRecord.value?.weekNo || '-')
const heroReporterLabel = computed(() => heroRecord.value?.realName || heroRecord.value?.username || '-')
const heroFlowNodes = computed(() => {
  if (showTransitionPlaceholder.value && isAuditTab.value) {
    return []
  }
  return buildFlowNodes(
    heroRecord.value || { status: heroStatus.value, currentApprovalNode: currentApprovalNode.value }
  )
})
const heroUpdatedAt = computed(() => formatDateTime(resolveRecordUpdatedAt(heroRecord.value || {
  ...editorForm,
  ...editorMeta
})))
const heroTaskCount = computed(() => recordTaskCount(heroRecord.value || {
  ...editorForm,
  ...editorMeta
}))
const heroReturnSummary = computed(() => recordReturnSummary(heroRecord.value))
const showHeroReturnSummary = computed(() => shouldShowReturnSummary(heroRecord.value) && Boolean(heroReturnSummary.value))
const currentFlowNodes = computed(() => buildFlowNodes(
  currentSourceRecord.value || { status: currentWeeklyStatus.value, currentApprovalNode: currentApprovalNode.value }
))
const detailFlowNodes = computed(() => buildFlowNodes(detailRecord.value || {}))
const returnSheetTargets = computed(() => deriveReturnTargets(returnSheetRecord.value))
const detailTasks = computed(() => parseTasksFromRecord(detailRecord.value))
const currentHeroEyebrow = computed(() => {
  if (isAuditTab.value) {
    return activeRecordTab.value === 'pending' ? '审核模式' : '审核回看模式'
  }
  return currentModeEyebrow.value
})
const currentHeroTitle = computed(() => {
  if (isAuditTab.value) {
    if (showTransitionPlaceholder.value) {
      return activeRecordTab.value === 'pending' ? '待我审核模式 · 正在切换' : '我已审核模式 · 正在切换'
    }
    if (!auditFocusRecord.value) {
      return activeRecordTab.value === 'pending' ? '待我审核模式' : '我已审核模式'
    }
    return `${recordCardTitle(auditFocusRecord.value)} · ${currentModeLabel.value}`
  }
  return currentModeTitle.value
})
const heroCurrentStageLabel = computed(() => {
  if (showTransitionPlaceholder.value) {
    return '正在同步当前阶段'
  }
  if (!heroRecord.value && isAuditTab.value) {
    return activeRecordTab.value === 'pending' ? '等待选择待审记录' : '等待选择已审记录'
  }
  return currentStageText(heroRecord.value || { status: heroStatus.value, currentApprovalNode: currentApprovalNode.value })
})
const heroNextStepLabel = computed(() => {
  if (showTransitionPlaceholder.value) {
    return '正在刷新下一步'
  }
  if (!heroRecord.value && isAuditTab.value) {
    return '请选择一条记录'
  }
  return nextFlowStepText(heroRecord.value || { status: heroStatus.value, currentApprovalNode: currentApprovalNode.value })
})
const heroReturnTargetLabel = computed(() => {
  if (showTransitionPlaceholder.value) {
    return '正在刷新退回目标'
  }
  if (!heroRecord.value && isAuditTab.value) {
    return '无退回目标'
  }
  return returnTargetText(heroRecord.value || { status: heroStatus.value, currentApprovalNode: currentApprovalNode.value })
})
const heroNotice = computed(() => {
  if (isAuditTab.value) {
    return auditModeHint.value
  }
  return currentModeNotice.value
})
const activeRecordBadgeLabel = computed(() => {
  if (activeRecordTab.value === 'pending') {
    return '当前查看'
  }
  if (activeRecordTab.value === 'processed') {
    return '当前回看'
  }
  return '当前周报'
})
const currentViewBannerTitle = computed(() => {
  if (showTransitionPlaceholder.value) {
    return activeRecordTab.value === 'pending' ? '当前查看：正在切换待审记录' : '当前回看：正在切换已审记录'
  }
  if (isMineTab.value) {
    return activeRecordId.value ? `当前填报：${recordCardTitle(currentSourceRecord.value || {})}` : '当前填报：新建周报'
  }
  if (!auditFocusRecord.value) {
    return activeRecordTab.value === 'pending' ? '当前查看：等待选择待审记录' : '当前查看：等待选择已审记录'
  }
  return `${activeRecordTab.value === 'pending' ? '当前查看' : '当前回看'}：${recordCardTitle(auditFocusRecord.value)}`
})
const currentViewBannerDesc = computed(() => {
  if (showTransitionPlaceholder.value) {
    return activeRecordTab.value === 'mine'
      ? '正在同步当前填报周报，请稍候。'
      : '顶部摘要、当前查看提示和详情区正在同步到最新焦点。'
  }
  if (isMineTab.value) {
    return activeRecordId.value
      ? '顶部和编辑区都绑定当前填报周报，可继续编辑或提交。'
      : '当前处于新建填报态，先补充任务内容后再暂存或提交。'
  }
  if (!auditFocusRecord.value) {
    return '审核分区顶部已切换为审核摘要模式，不再显示填报编辑区。'
  }
  return `${heroCurrentStageLabel.value} · ${heroNextStepLabel.value} · ${heroReturnTargetLabel.value}`
})
const auditPlaceholderTitle = computed(() => {
  if (focusTransitionLabel.value) {
    return focusTransitionLabel.value
  }
  if (!currentTabRecords.value.length) {
    return activeRecordTab.value === 'pending' ? '待审记录已处理完毕' : '当前分区暂无可回看记录'
  }
  return activeRecordTab.value === 'pending' ? '正在切到下一条待审记录' : '正在切到新的回看记录'
})
const auditPlaceholderDesc = computed(() => {
  if (!currentTabRecords.value.length) {
    return '顶部已切到审核模式空态，旧摘要不会继续停留。'
  }
  return focusTransitionLabel.value
    ? `${focusTransitionLabel.value}，顶部摘要和详情区会先进入过渡占位。`
    : '顶部摘要和详情区会先进入过渡占位，再落到新的选中记录。'
})
const detailLoadingLabel = computed(() => {
  if (focusTransitioning.value) {
    return activeRecordTab.value === 'pending' ? '正在切换待审记录...' : '正在切换当前记录...'
  }
  return '加载中...'
})
const detailLoadingDesc = computed(() => {
  if (focusTransitioning.value) {
    return '旧详情已收起，正在同步新的顶部摘要、当前查看提示和详情内容。'
  }
  return '正在加载最新周报详情，请稍候。'
})

function recordsForTab(tabKey) {
  if (tabKey === 'pending') {
    return pendingReviewRecords.value
  }
  if (tabKey === 'processed') {
    return processedReviewRecords.value
  }
  return myWeeklyRecords.value
}

function resolveFocusRecordForTab(tabKey, preferredId = null, options = {}) {
  const { excludeId = null } = options
  const records = recordsForTab(tabKey)
  if (preferredId) {
    const preferred = records.find((item) => Number(item.id) === Number(preferredId))
    if (preferred) {
      return preferred
    }
  }
  const filtered = excludeId ? records.filter((item) => Number(item.id) !== Number(excludeId)) : records
  return filtered[0] || records[0] || null
}

function clearDetailContext() {
  detailVisible.value = false
  detailLoading.value = false
  detailRecord.value = null
}

function showDetailEmptyState() {
  detailVisible.value = true
  detailLoading.value = false
  detailRecord.value = null
}

function beginFocusTransition(label = '') {
  focusTransitioning.value = true
  focusTransitionLabel.value = label
}

function endFocusTransition() {
  focusTransitioning.value = false
  focusTransitionLabel.value = ''
}

function resolveDebugFocusRecord() {
  if (isAuditTab.value) {
    return auditFocusRecord.value || detailRecord.value || activeRecord.value || null
  }
  return currentSourceRecord.value || activeRecord.value || null
}

function logWeeklyWorkFocus(event, payload = {}) {
  if (!DEV_FOCUS_DEBUG) {
    return
  }
  const record = payload.record || resolveDebugFocusRecord()
  const tab = payload.tab || activeRecordTab.value
  const focusId = payload.focusId ?? record?.id ?? activeRecordId.value ?? 'null'
  const stage = payload.stage || currentStageText(record)
  const nextStep = payload.nextStep || nextFlowStepText(record)
  const returnTarget = payload.returnTarget || returnTargetText(record)
  const transition = payload.transition ?? (focusTransitioning.value ? focusTransitionLabel.value || 'true' : 'false')
  const suffix = payload.extra ? `, ${payload.extra}` : ''
  console.log(
    `[WeeklyWorkFocus] event=${event}, tab=${tab}, focusId=${focusId}, stage=${stage}, nextStep=${nextStep}, returnTarget=${returnTarget}, transition=${transition}${suffix}`
  )
}
const approvalTimelineEntries = computed(() => {
  const record = detailRecord.value
  if (!record) {
    return []
  }
  const logs = [...extractApprovalLogs(record)]
  const entries = logs.map((log, index) => ({
    id: log.id ?? `log-${index}`,
    action: log.action,
    actionLabel: approvalActionLabel(log.action, record),
    fromNode: log.fromNode,
    toNode: log.toNode,
    reviewerName: log.reviewerName,
    createTime: log.createTime,
    comment: log.comment,
    title: approvalActionTitle(log, record)
  }))
  if (record.submitTime) {
    const flowNodes = buildFlowNodes(record)
    const submitTargetNode = flowNodes[1]?.key || resolveCurrentApprovalNode(record)
    const submitSourceNode = flowNodes[0]?.key || resolveCurrentApprovalNode(record)
    entries.push({
      id: `submit-${record.id}`,
      action: 'SUBMIT',
      actionLabel: approvalActionLabel('SUBMIT'),
      fromNode: submitSourceNode,
      toNode: submitTargetNode,
      reviewerName: record.realName || record.username || '填报人',
      createTime: record.submitTime,
      comment: '提交周报进入审批流',
      title: approvalActionTitle({ action: 'SUBMIT', fromNode: submitSourceNode, toNode: submitTargetNode }, record)
    })
  }
  if (record.status === 'RETURNED' && canEdit(record)) {
    entries.unshift({
      id: `modify-${record.id}`,
      action: 'MODIFY',
      actionLabel: approvalActionLabel('MODIFY'),
      fromNode: resolveCurrentApprovalNode(record),
      toNode: resolveCurrentApprovalNode(record),
      reviewerName: record.realName || record.username || '填报人',
      createTime: record.lastReviewTime || record.createTime,
      comment: '当前周报处于审批修改模式，可修改后再次提交',
      title: approvalActionTitle({ action: 'MODIFY' }, record)
    })
  }
  return entries.sort((left, right) => String(right.createTime || '').localeCompare(String(left.createTime || '')))
})
const editorDirty = computed(() => {
  return buildEditorSnapshot() !== editorSnapshot.value
})

function createEmptyEditor() {
  return {
    id: null,
    weekNo: getCurrentWeekNo(),
    workPlan: '',
    workContent: '',
    remark: ''
  }
}

function createDefaultEditorMeta() {
  return {
    status: 'DRAFT',
    currentApprovalNode: 'STAFF',
    lastReturnTarget: '',
    lastReturnComment: ''
  }
}

function createEmptyTaskDraft() {
  return {
    nature: 'SELF',
    receiveTime: '',
    deadline: '',
    progress: '',
    assistance: ''
  }
}

function normalizeDateInput(value) {
  const text = String(value || '').trim()
  return /^\d{4}-\d{2}-\d{2}$/.test(text) ? text : ''
}

function formatDateKey(value) {
  if (!value) {
    return ''
  }
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function getMonthStart(value = new Date()) {
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return new Date(new Date().getFullYear(), new Date().getMonth(), 1)
  }
  return new Date(date.getFullYear(), date.getMonth(), 1)
}

function resolveRecordCreateDate(record = null) {
  return normalizeDateInput(formatDateKey(record?.createTime || record?.submitTime || record?.updateTime || ''))
}

function compactCalendarTaskText(text, fallback = '任务') {
  const normalized = String(text || '').replace(/\s+/g, ' ').trim()
  if (!normalized) {
    return fallback
  }
  return normalized.length > 8 ? `${normalized.slice(0, 8)}...` : normalized
}

function buildCalendarPreviewItems(records = []) {
  const previewItems = []
  for (const record of records) {
    const tasks = parseTasksFromRecord(record)
    if (!tasks.length) {
      previewItems.push(compactCalendarTaskText(record?.weekNo, '周报'))
      continue
    }
    for (const task of tasks) {
      const label = compactCalendarTaskText(task.progress || taskNatureLabel(task.nature), taskNatureLabel(task.nature))
      if (label && !previewItems.includes(label)) {
        previewItems.push(label)
      }
      if (previewItems.length >= 3) {
        return previewItems
      }
    }
  }
  return previewItems
}

function buildCalendarDays(monthStart, recordMap = {}, selectedDate = '') {
  const cursor = getMonthStart(monthStart)
  const firstDay = cursor.getDay() === 0 ? 7 : cursor.getDay()
  const startDate = new Date(cursor)
  startDate.setDate(cursor.getDate() - (firstDay - 1))
  return Array.from({ length: 42 }, (_, index) => {
    const day = new Date(startDate)
    day.setDate(startDate.getDate() + index)
    const dateKey = formatDateKey(day)
    const dayRecords = recordMap[dateKey] || []
    const previewItems = buildCalendarPreviewItems(dayRecords)
    return {
      key: dateKey || `empty-${index}`,
      dateKey,
      dateNumber: day.getDate(),
      isCurrentMonth: day.getMonth() === cursor.getMonth(),
      isToday: dateKey === formatDateKey(new Date()),
      isSelected: dateKey === selectedDate,
      recordCount: dayRecords.length,
      previewItems: previewItems.slice(0, 2),
      moreCount: Math.max(previewItems.length - 2, 0)
    }
  })
}

function chunkCalendarWeeks(days = []) {
  const weeks = []
  for (let index = 0; index < days.length; index += 7) {
    weeks.push(days.slice(index, index + 7))
  }
  return weeks
}

function getWeekNoFromDate(value = new Date()) {
  const source = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  if (Number.isNaN(source.getTime())) {
    return getCurrentWeekNo()
  }
  const target = new Date(source.valueOf())
  const dayNr = (source.getDay() + 6) % 7
  target.setDate(target.getDate() - dayNr + 3)
  const firstThursday = new Date(target.getFullYear(), 0, 4)
  const firstDayNr = (firstThursday.getDay() + 6) % 7
  firstThursday.setDate(firstThursday.getDate() - firstDayNr + 3)
  const week = 1 + Math.round((target - firstThursday) / 604800000)
  return `${target.getFullYear()}-W${String(week).padStart(2, '0')}`
}

function getCurrentWeekNo() {
  return getWeekNoFromDate(new Date())
}

function statusLabel(status, record = null) {
  if (isWorkflowApproved(record || { status })) {
    return '已通过'
  }
  if (['SUBMITTED', 'PENDING_SECTION_CHIEF', 'PENDING_DEPUTY_LEADER', 'PENDING_LEGION_LEADER'].includes(status)) {
    return `待${approvalNodeLabel(resolveCurrentApprovalNode(record || { status }), record)}处理`
  }
  return {
    DRAFT: '草稿',
    APPROVED: '已通过',
    RETURNED: '已退回'
  }[status] || status || '-'
}

function statusTagType(status) {
  return {
    DRAFT: 'primary',
    SUBMITTED: 'warning',
    PENDING_SECTION_CHIEF: 'warning',
    PENDING_DEPUTY_LEADER: 'warning',
    PENDING_LEGION_LEADER: 'warning',
    APPROVED: 'success',
    RETURNED: 'danger'
  }[status] || 'default'
}

function resolveFlowNodeMeta(code, record = null) {
  const customNodes = extractRawFlowNodes(record)
  if (Array.isArray(customNodes)) {
    const matched = customNodes.find((item) => {
      const key = typeof item === 'string'
        ? item
        : item?.key || item?.code || item?.nodeCode || item?.roleCode || item?.value
      return String(key || '') === String(code || '')
    })
    if (matched && typeof matched === 'object') {
      return {
        label: buildFlowNodeLabel(matched, code, record),
        order: Number(matched.order || matched.sort || matched.seq || matched.index || LEGACY_FLOW_NODE_META[code]?.order || 0),
        roleCode: matched.roleCode || matched.code || matched.key || code
      }
    }
  }
  const orgFlowNodes = buildFlowNodesFromOrgTree(record || {})
  const orgMatchedNode = orgFlowNodes.find((node) => matchesFlowNode(node, code))
  if (orgMatchedNode) {
    return {
      label: orgMatchedNode.label,
      order: Number(orgMatchedNode.order || 0),
      roleCode: orgMatchedNode.roleCode || orgMatchedNode.nodeCode || code
    }
  }
  return LEGACY_FLOW_NODE_META[code] || {
    label: code || '-',
    order: LEGACY_FLOW_SEQUENCE.indexOf(code) + 1 || 1,
    roleCode: code || ''
  }
}

function normalizeLabelValue(value) {
  const text = String(value ?? '').trim()
  if (!text || ['null', 'undefined', '[object Object]'].includes(text)) {
    return ''
  }
  return text
}

function pickFirstLabel(source, fields) {
  if (!source || typeof source !== 'object') {
    return ''
  }
  for (const field of fields) {
    const value = normalizeLabelValue(source[field])
    if (value) {
      return value
    }
  }
  return ''
}

function isGenericNodeLabel(label) {
  return LEGACY_GENERIC_LABEL_SET.has(normalizeLabelValue(label))
}

function toCamelCase(value) {
  return String(value || '')
    .toLowerCase()
    .split('_')
    .filter(Boolean)
    .map((segment, index) => index === 0 ? segment : `${segment.slice(0, 1).toUpperCase()}${segment.slice(1)}`)
    .join('')
}

function collectRecordScopedNodeSources(record, code) {
  if (!record || typeof record !== 'object') {
    return []
  }
  const sources = [record]
  const codePrefix = toCamelCase(code)
  const fieldPrefixes = [codePrefix, `${codePrefix}Node`, `${codePrefix}Approver`, `${codePrefix}Approval`]
  for (const prefix of fieldPrefixes) {
    const nested = record[prefix]
    if (nested && typeof nested === 'object') {
      sources.push(nested)
    }
    const summary = {}
    for (const field of [...REAL_POST_LABEL_FIELDS, ...DISPLAY_LABEL_FIELDS, ...PERSON_LABEL_FIELDS]) {
      const key = `${prefix}${field.slice(0, 1).toUpperCase()}${field.slice(1)}`
      if (record[key] !== undefined && record[key] !== null && record[key] !== '') {
        summary[field] = record[key]
      }
    }
    if (Object.keys(summary).length) {
      sources.push(summary)
    }
  }
  const recordUserId = resolveRecordUserId(record)
  const recordOwnerKeys = [`USER_${recordUserId}`, 'STAFF']
  if (recordUserId) {
    recordOwnerKeys.push(String(recordUserId))
  }
  if (recordOwnerKeys.includes(String(code || ''))) {
    sources.push({
      postName: record.reporterPostName || record.submitterPostName || record.creatorPostName || record.userPostName || record.jobTitle,
      realName: record.realName,
      username: record.username
    })
    sources.push(userStore.userInfo || {})
  }
  if (String(code || '') === String(resolveCurrentApprovalNode(record))) {
    sources.push({
      postName: record.currentApprovalNodePostName || record.currentNodePostName,
      positionName: record.currentApprovalNodePositionName || record.currentNodePositionName,
      orgRoleName: record.currentApprovalNodeOrgRoleName || record.currentNodeOrgRoleName,
      label: record.currentApprovalNodeLabel || record.currentNodeLabel,
      name: record.currentApprovalNodeName || record.currentNodeName
    })
  }
  return sources
}

function composePersonWithPost(source) {
  const postLabel = pickFirstLabel(source, REAL_POST_LABEL_FIELDS)
  const personLabel = pickFirstLabel(source, PERSON_LABEL_FIELDS)
  if (personLabel && postLabel) {
    return `${personLabel}（${postLabel}）`
  }
  return postLabel || personLabel || ''
}

function buildFlowNodeLabel(node, code, record = null) {
  const sources = [
    node && typeof node === 'object' ? node : null,
    ...collectRecordScopedNodeSources(record, code)
  ].filter(Boolean)

  for (const source of sources) {
    const postLabel = pickFirstLabel(source, REAL_POST_LABEL_FIELDS)
    if (postLabel) {
      return postLabel
    }
  }

  for (const source of sources) {
    const displayLabel = pickFirstLabel(source, DISPLAY_LABEL_FIELDS)
    if (displayLabel && !isGenericNodeLabel(displayLabel)) {
      return displayLabel
    }
  }

  for (const source of sources) {
    const personWithPost = composePersonWithPost(source)
    if (personWithPost) {
      return personWithPost
    }
  }

  for (const source of sources) {
    const displayLabel = pickFirstLabel(source, DISPLAY_LABEL_FIELDS)
    if (displayLabel) {
      return displayLabel
    }
  }

  return LEGACY_FLOW_NODE_META[code]?.label || normalizeLabelValue(code) || '-'
}

function approvalNodeLabel(node, record = null) {
  if (!node) {
    return '-'
  }
  if (typeof node === 'object') {
    if (shouldHideFlowDisplayNode(node)) {
      return '-'
    }
    return buildFlowNodeLabel(node, node.key || node.code || node.roleCode, record)
  }
  if (node === 'APPROVED') {
    return '已完成'
  }
  const matchedDisplayNode = findDisplayFlowNode(record, node)
  if (matchedDisplayNode?.label) {
    return matchedDisplayNode.label
  }
  const meta = resolveFlowNodeMeta(node, record)
  return shouldHideFlowDisplayNode(meta) ? '-' : (meta.label || node)
}

function approvalActionLabel(action, record = null) {
  return {
    SUBMIT: '提交审核',
    APPROVE: isFinalApprovalStep(record) ? '审批完成' : '提交下一级',
    RETURN: '退回上一级',
    MODIFY: '修改后提交'
  }[action] || action || '-'
}

function approvalActionTitle(log, record = null) {
  if (log.action === 'SUBMIT') {
    return `由 ${approvalNodeLabel(log.fromNode, record)} 提交至 ${approvalNodeLabel(log.toNode, record)}`
  }
  if (log.action === 'APPROVE') {
    return `由 ${approvalNodeLabel(log.fromNode, record)} 审批通过，流转至 ${approvalTargetLabel(log, record)}`
  }
  if (log.action === 'RETURN') {
    return `由 ${approvalNodeLabel(log.fromNode, record)} 退回给 ${approvalNodeLabel(log.toNode, record)}`
  }
  if (log.action === 'MODIFY') {
    return '修改后重新提交审批'
  }
  return approvalActionLabel(log.action)
}

function approvalTargetLabel(log, record = null) {
  if (!log?.toNode) {
    return '-'
  }
  if (String(log.toNode) === 'APPROVED') {
    return '流程已完成'
  }
  return approvalNodeLabel(log.toNode, record)
}

function recordCardTitle(record = null) {
  if (activeRecordTab.value === 'mine') {
    return record?.weekNo || '-'
  }
  return `${record?.weekNo || '-'} · ${record?.realName || record?.username || '-'}`
}

function extractRawFlowNodes(record = {}) {
  return record?.flowNodes || record?.approvalFlowNodes || record?.flowNodeList || record?.approvalNodes || null
}

function extractApprovalLogs(record = null) {
  if (!record || typeof record !== 'object') {
    return []
  }
  const logCandidates = [
    record.approvalLogs,
    record.approvalRecords,
    record.approvalLogList,
    record.approvalRecordList,
    record.logs,
    record.logList
  ]
  const matchedLogs = logCandidates.find((item) => Array.isArray(item))
  return Array.isArray(matchedLogs) ? matchedLogs.filter((item) => item && typeof item === 'object') : []
}

function normalizeWeeklyWorkRecord(record = null) {
  if (!record || typeof record !== 'object') {
    return record
  }
  const hasApprovalLogField = [
    'approvalLogs',
    'approvalRecords',
    'approvalLogList',
    'approvalRecordList',
    'logs',
    'logList'
  ].some((field) => Object.prototype.hasOwnProperty.call(record, field))
  if (!hasApprovalLogField) {
    return { ...record }
  }
  return {
    ...record,
    approvalLogs: extractApprovalLogs(record)
  }
}

function normalizeOrgChainNode(node = {}, index = 0) {
  const userId = Number(node?.id ?? node?.userId ?? node?.value ?? 0) || null
  return {
    ...node,
    userId,
    id: userId,
    key: node?.key || node?.nodeCode || node?.code || (userId ? `USER_${userId}` : `ORG_NODE_${index + 1}`),
    parentUserId: node?.parentUserId == null ? null : Number(node.parentUserId),
    levelNo: Number(node?.levelNo ?? node?.level ?? node?.rank ?? index + 1),
    treePath: node?.treePath || node?.path || node?.hierarchyPath || '',
    realName: node?.realName || node?.name || '',
    username: node?.username || node?.userName || '',
    jobTitle: node?.jobTitle || node?.postName || node?.positionName || node?.orgRoleName || '',
    roleCode: node?.roleCode || node?.nodeCode || node?.code || ''
  }
}

function resolveRecordUserId(record = null) {
  const candidate = record?.userId || record?.reporterUserId || record?.submitterUserId || record?.creatorUserId || currentUserId.value
  return Number(candidate || 0) || 0
}

function getCachedOrgChain(userId) {
  if (!userId) {
    return []
  }
  return Array.isArray(orgChainMap[userId]) ? orgChainMap[userId] : []
}

async function ensureOrgChainLoaded(userId) {
  const normalizedUserId = Number(userId || 0) || 0
  if (!normalizedUserId) {
    return []
  }
  if (getCachedOrgChain(normalizedUserId).length) {
    return getCachedOrgChain(normalizedUserId)
  }
  if (orgChainRequestMap.has(normalizedUserId)) {
    return orgChainRequestMap.get(normalizedUserId)
  }
  const request = queryOrgAncestors(normalizedUserId)
    .then((response) => {
      const normalized = (Array.isArray(response?.data) ? response.data : [])
        .map((item, index) => normalizeOrgChainNode(item, index))
        .filter((item) => item.id || item.treePath || item.jobTitle)
      orgChainMap[normalizedUserId] = normalized
      return normalized
    })
    .catch(() => [])
    .finally(() => {
      orgChainRequestMap.delete(normalizedUserId)
    })
  orgChainRequestMap.set(normalizedUserId, request)
  return request
}

async function warmupOrgChains(records = []) {
  const userIds = [...new Set((records || []).map((item) => resolveRecordUserId(item)).filter(Boolean))]
  if (!userIds.length) {
    return
  }
  await Promise.all(userIds.map((userId) => ensureOrgChainLoaded(userId)))
}

async function fetchCurrentUserOrgChildren() {
  if (!currentUserId.value) {
    orgChildCount.value = 0
    return
  }
  try {
    const response = await queryOrgChildren(currentUserId.value)
    orgChildCount.value = Array.isArray(response?.data) ? response.data.length : 0
  } catch (error) {
    orgChildCount.value = null
  }
}

function extractRecordOrgChain(record = {}) {
  const rawChain = record?.orgChain || record?.ancestorChain || record?.ancestors || record?.hierarchyNodes || record?.orgNodes || record?.reporterChain
  if (!Array.isArray(rawChain)) {
    return []
  }
  return rawChain
    .map((item, index) => normalizeOrgChainNode(item, index))
    .filter((item) => item.id || item.treePath || item.jobTitle)
}

function orderOrgChainForApproval(nodes = []) {
  const uniqueNodes = []
  const seen = new Set()
  for (const node of nodes) {
    const identity = node.id || node.key || `${node.levelNo}-${node.jobTitle}-${node.realName}-${node.username}`
    if (seen.has(identity)) {
      continue
    }
    seen.add(identity)
    uniqueNodes.push(node)
  }
  const sorted = [...uniqueNodes].sort((left, right) => {
    const levelGap = Number(left.levelNo || 0) - Number(right.levelNo || 0)
    if (levelGap !== 0) {
      return levelGap
    }
    return String(left.treePath || '').localeCompare(String(right.treePath || ''))
  })
  return sorted.reverse()
}

function collectOrgChainCandidates(record = {}) {
  const userId = resolveRecordUserId(record)
  const candidates = []
  const recordChain = extractRecordOrgChain(record)
  if (recordChain.length) {
    candidates.push({ source: 'record_chain', nodes: recordChain })
  }
  const cachedChain = getCachedOrgChain(userId)
  if (cachedChain.length) {
    candidates.push({ source: 'orgtree_ancestors', nodes: cachedChain })
  }
  if (!recordChain.length) {
    const currentNode = normalizeOrgChainNode({
      id: userId || currentUserId.value,
      realName: record?.realName || userStore.userInfo?.realName,
      username: record?.username || userStore.userInfo?.username,
      jobTitle: record?.jobTitle || record?.postName || userStore.userInfo?.jobTitle || userStore.userInfo?.postName
    })
    if (currentNode.id || currentNode.jobTitle || currentNode.realName) {
      candidates.push({ source: 'record_owner', nodes: [currentNode] })
    }
  }
  return candidates
}

function resolveLegacyAliasesForOrgNode(index, total) {
  const aliases = []
  if (index === 0) {
    aliases.push('STAFF')
  }
  if (index === 1 && total >= 2) {
    aliases.push('SECTION_CHIEF')
  }
  if (total >= 4 && index === total - 2) {
    aliases.push('DEPUTY_LEADER')
  }
  if (index === total - 1 && total >= 2) {
    aliases.push(total >= 4 ? 'LEGION_LEADER' : total === 3 ? 'LEGION_LEADER' : 'SECTION_CHIEF')
  }
  return aliases.filter((alias, aliasIndex) => aliases.indexOf(alias) === aliasIndex)
}

function buildFlowNodesFromOrgTree(record = {}) {
  const orgChainCandidates = collectOrgChainCandidates(record)
  const orgChainCandidate = orgChainCandidates.find((candidate) => candidate.nodes.length >= 2)
    || orgChainCandidates.find((candidate) => candidate.nodes.length)
  if (!orgChainCandidate?.nodes?.length) {
    return []
  }
  const orderedChain = orderOrgChainForApproval(orgChainCandidate.nodes)
  return orderedChain.map((node, index) => ({
    key: node.key || (node.userId ? `USER_${node.userId}` : `ORG_NODE_${index + 1}`),
    label: buildFlowNodeLabel(node, node.key || node.roleCode || `ORG_NODE_${index + 1}`, record),
    name: node.name || node.realName || node.username || '',
    order: index + 1,
    roleCode: node.roleCode || node.jobTitle || node.postName || '',
    nodeCode: node.nodeCode || node.code || '',
    userId: node.userId || null,
    source: orgChainCandidate.source,
    aliases: resolveLegacyAliasesForOrgNode(index, orderedChain.length)
  }))
}

function buildLegacyFlowNodes(record = {}) {
  const currentNode = resolveCurrentApprovalNode(record)
  const fallbackCodes = [...LEGACY_FLOW_SEQUENCE]
  const availableTargets = Array.isArray(record?.availableReturnTargets) ? record.availableReturnTargets : []
  const mergedCodes = [...fallbackCodes, ...availableTargets, currentNode].filter(Boolean)
  const uniqueCodes = mergedCodes.filter((code, index) => mergedCodes.indexOf(code) === index)
  return uniqueCodes.map((code, index) => ({
    ...normalizeFlowNode(code, index, record),
    aliases: [code],
    source: 'legacy'
  }))
}

function matchesFlowNode(node, targetCode) {
  if (!node || !targetCode) {
    return false
  }
  const candidates = [
    node.key,
    node.roleCode,
    node.nodeCode,
    ...(Array.isArray(node.aliases) ? node.aliases : [])
  ].filter(Boolean).map((item) => String(item))
  return candidates.includes(String(targetCode))
}

function findFlowNodeIndex(flowNodes = [], targetCode) {
  return flowNodes.findIndex((node) => matchesFlowNode(node, targetCode))
}

function normalizeFlowNode(node, index, record = null) {
  if (typeof node === 'string') {
    const meta = resolveFlowNodeMeta(node, record)
    return {
      key: node,
      label: meta.label,
      name: meta.name || '',
      order: meta.order || index + 1,
      roleCode: meta.roleCode || node
    }
  }
  const key = node?.key || node?.code || node?.nodeCode || node?.roleCode || node?.value || `NODE_${index + 1}`
  const meta = resolveFlowNodeMeta(key, record)
  return {
    key,
    label: buildFlowNodeLabel(node, key, record) || meta.label,
    name: normalizeLabelValue(node?.name) || normalizeLabelValue(node?.label) || meta.name || '',
    order: Number(node?.order || node?.sort || node?.seq || node?.index || meta.order || index + 1),
    roleCode: node?.roleCode || key,
    userId: Number(node?.userId || node?.id || 0) || null,
    aliases: Array.isArray(node?.aliases) ? node.aliases.filter(Boolean) : []
  }
}

function shouldHideFlowDisplayNode(node = {}) {
  const roleCode = String(node?.roleCode || '').trim().toUpperCase()
  if (roleCode === 'SUPER_ADMIN') {
    return true
  }
  const aliases = Array.isArray(node?.aliases) ? node.aliases.map((item) => String(item || '').trim().toUpperCase()) : []
  if (aliases.includes('SUPER_ADMIN') || String(node?.key || '').trim().toUpperCase() === 'SUPER_ADMIN') {
    return true
  }
  const candidateNames = [
    node?.name,
    node?.label,
    node?.displayLabel,
    node?.nodeLabel
  ].map((value) => normalizeLabelValue(value))
  return candidateNames.includes('超级管理员')
}

function findDisplayFlowNode(record = null, targetCode = null) {
  if (!targetCode) {
    return null
  }
  return buildFlowNodes(record || {}).find((node) => matchesFlowNode(node, targetCode)) || null
}

function resolveRecordOriginNode(record = null) {
  const originNode = buildFlowNodes(record || {}).at(0)
  if (!originNode) {
    return LEGACY_FLOW_SEQUENCE[0]
  }
  return (originNode.aliases || []).find((alias) => LEGACY_FLOW_SEQUENCE.includes(alias)) || originNode.key
}

function isOriginApprovalNode(record = null, nodeCode = null) {
  const flowNodes = buildFlowNodes(record || {})
  const originNode = flowNodes[0]
  return matchesFlowNode(originNode, nodeCode || resolveCurrentApprovalNode(record || {}))
}

function buildFlowNodes(record = {}) {
  const rawNodes = extractRawFlowNodes(record)
  let baseNodes = []
  if (Array.isArray(rawNodes) && rawNodes.length) {
    baseNodes = rawNodes.map((node, index) => ({
      ...normalizeFlowNode(node, index, record),
      aliases: [
        ...(Array.isArray(node?.aliases) ? node.aliases : []),
        node?.key,
        node?.code,
        node?.nodeCode,
        node?.roleCode,
        node?.value
      ].filter((value, index, array) => value && array.indexOf(value) === index),
      source: 'record_flow'
    }))
  }
  if (!baseNodes.length) {
    baseNodes = buildFlowNodesFromOrgTree(record)
  }
  if (!baseNodes.length) {
    baseNodes = buildLegacyFlowNodes(record)
  }
  const currentNode = resolveCurrentApprovalNode(record)
  if (currentNode && findFlowNodeIndex(baseNodes, currentNode) < 0) {
    baseNodes.push({
      ...normalizeFlowNode(currentNode, baseNodes.length, record),
      aliases: [currentNode],
      source: 'current_node_fallback'
    })
  }
  const sortedNodes = [...baseNodes]
    .filter((node) => !shouldHideFlowDisplayNode(node))
    .sort((left, right) => left.order - right.order)
  const activeIndex = findFlowNodeIndex(sortedNodes, currentNode)
  const originKey = sortedNodes[0]?.key
  return sortedNodes.map((node, index) => {
    let state = 'pending'
    if (isWorkflowApproved(record)) {
      state = 'done'
    } else if ((record?.status === 'DRAFT' || !record?.status) && node.key === originKey) {
      state = 'current'
    } else if (record?.status === 'RETURNED' && index === activeIndex) {
      state = 'returned'
    } else if (activeIndex >= 0 && index < activeIndex) {
      state = 'done'
    } else if (index === activeIndex) {
      state = 'current'
    }
    return {
      ...node,
      state
    }
  })
}

function taskNatureLabel(nature) {
  return {
    UPPER: '上级',
    GROUP: '本团',
    SECTION: '本科（室）',
    SELF: '自行'
  }[nature] || '自行'
}

function summarizeTask(record) {
  const tasks = parseTasksFromRecord(record)
  if (!tasks.length) {
    return '暂无任务'
  }
  const first = tasks[0]
  const nature = taskNatureLabel(first.nature || 'SELF')
  const progress = first.progress || '暂无进展'
  const suffix = tasks.length > 1 ? ` 等 ${tasks.length} 项任务` : ''
  return `${nature} · ${progress}${suffix}`
}

function recordTaskCount(record) {
  return parseTasksFromRecord(record).length || 1
}

function recordReturnSummary(record) {
  const latestReturnLog = findLatestReturnLog(record)
  if (!latestReturnLog && !record?.lastReturnComment) {
    return ''
  }
  const sourceLabel = approvalNodeLabel(latestReturnLog?.fromNode, record)
  const targetLabel = approvalNodeLabel(latestReturnLog?.toNode || record?.lastReturnTarget, record)
  const comment = latestReturnLog?.comment || record?.lastReturnComment || ''
  const prefix = sourceLabel && sourceLabel !== '-' ? `${sourceLabel} 退回至 ${targetLabel}` : targetLabel
  const message = comment ? `${prefix} · ${comment}` : prefix
  return message.length > 34 ? `${message.slice(0, 34)}...` : message
}

function findLatestReturnLog(record = null) {
  const approvalLogs = extractApprovalLogs(record)
  if (!approvalLogs.length) {
    return null
  }
  return [...approvalLogs]
    .filter((log) => log?.action === 'RETURN')
    .sort((left, right) => String(right.createTime || '').localeCompare(String(left.createTime || '')))[0] || null
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

function resolveRecordUpdatedAt(record = {}) {
  return record?.lastReviewTime || record?.updateTime || record?.submitTime || record?.createTime || ''
}

function shouldShowReturnSummary(record = null) {
  return record?.status === 'RETURNED'
}

function fillEditor(record) {
  editorForm.id = record.id ?? null
  editorForm.weekNo = record.weekNo || getCurrentWeekNo()
  editorForm.workPlan = record.workPlan || ''
  editorForm.workContent = record.workContent || ''
  editorForm.remark = record.remark || ''
  syncTaskListFromEditor()
  applyEditorMeta(record)
}

function buildEditorSnapshot() {
  return JSON.stringify({
    id: editorForm.id || null,
    weekNo: editorForm.weekNo || '',
    workPlan: editorForm.workPlan || '',
    workContent: editorForm.workContent || '',
    remark: editorForm.remark || '',
    status: editorMeta.status || '',
    currentApprovalNode: editorMeta.currentApprovalNode || '',
    approvalRevisionMode: approvalRevisionMode.value
  })
}

function commitEditorSnapshot() {
  editorSnapshot.value = buildEditorSnapshot()
}

function resetEditor() {
  Object.assign(editorForm, createEmptyEditor())
  editorMode.value = 'create'
  activeRecordId.value = null
  taskList.value = []
  Object.assign(taskDraft, createEmptyTaskDraft())
  editingTaskIndex.value = -1
  Object.assign(editorMeta, createDefaultEditorMeta())
  approvalRevisionMode.value = false
  commitEditorSnapshot()
}

function applyEditorMeta(record = null) {
  editorMeta.status = record?.status || 'DRAFT'
  editorMeta.currentApprovalNode = resolveCurrentApprovalNode(record || {})
  editorMeta.lastReturnTarget = record?.lastReturnTarget || ''
  editorMeta.lastReturnComment = record?.lastReturnComment || ''
}

function extractFieldValue(source, label) {
  const match = String(source || '').match(new RegExp(`${label}[：:]([^｜\\n]+)`))
  return match?.[1]?.trim() || ''
}

function normalizeTask(task = {}) {
  return {
    nature: task.nature || 'SELF',
    receiveTime: task.receiveTime || '',
    deadline: task.deadline || '',
    progress: task.progress || '',
    assistance: task.assistance || ''
  }
}

function parseTasksFromRecord(record = {}) {
  const workPlan = String(record?.workPlan || '')
  if (workPlan.startsWith(TASKS_MARKER)) {
    try {
      const parsed = JSON.parse(workPlan.slice(TASKS_MARKER.length))
      if (Array.isArray(parsed)) {
        return parsed.map((task) => normalizeTask(task)).filter(hasTaskContent)
      }
    } catch (error) {
      return []
    }
  }
  const legacyTask = normalizeTask({
    nature: extractFieldValue(workPlan, '任务性质') || 'SELF',
    receiveTime: extractFieldValue(workPlan, '领取时间'),
    deadline: extractFieldValue(workPlan, '完成时限'),
    progress: record?.workContent || '',
    assistance: record?.remark || ''
  })
  return hasTaskContent(legacyTask) ? [legacyTask] : []
}

function hasTaskContent(task = {}) {
  return Boolean(task.receiveTime || task.deadline || task.progress || task.assistance)
}

function syncTaskListFromEditor() {
  taskList.value = parseTasksFromRecord(editorForm)
}

function fillCurrentRecord(record, mode = 'view') {
  fillEditor(record)
  editorMode.value = mode
  activeRecordId.value = record?.id ?? null
  commitEditorSnapshot()
}

function buildTaskSummary(tasks) {
  return tasks.map((task, index) => `任务${index + 1}：${taskNatureLabel(task.nature)}｜${task.progress || '暂无进展'}`).join('\n')
}

function buildAssistSummary(tasks) {
  return tasks.map((task, index) => `任务${index + 1}：${task.assistance || '无'}`).join('\n')
}

function syncEditorFromTaskList() {
  const normalizedTasks = taskList.value.map((task) => normalizeTask(task)).filter(hasTaskContent)
  taskList.value = normalizedTasks
  editorForm.workPlan = normalizedTasks.length ? `${TASKS_MARKER}${JSON.stringify(normalizedTasks)}` : ''
  editorForm.workContent = normalizedTasks.length ? buildTaskSummary(normalizedTasks) : ''
  editorForm.remark = normalizedTasks.length ? buildAssistSummary(normalizedTasks) : ''
}

async function confirmDiscardIfDirty(message = '当前有未保存修改，确认继续吗？') {
  if (!editorDirty.value) {
    return true
  }
  try {
    await showConfirmDialog({
      title: '未保存修改',
      message
    })
    return true
  } catch (error) {
    return false
  }
}

async function handleDeleteTask(index) {
  if (!canEditCurrentWeekly.value || index < 0 || index >= taskList.value.length) {
    return
  }
  try {
    await showConfirmDialog({
      title: '删除当前任务',
      message: '确认删除当前周报任务内容吗？'
    })
  } catch (error) {
    return
  }
  taskList.value.splice(index, 1)
  syncEditorFromTaskList()
  showToast('任务已删除')
}

function openTaskEditor(index = -1) {
  if (pageBusy.value || !canEditCurrentWeekly.value) {
    return
  }
  editingTaskIndex.value = index
  Object.assign(taskDraft, index >= 0 ? normalizeTask(taskList.value[index]) : createEmptyTaskDraft())
  taskEditorVisible.value = true
}

function closeTaskEditor() {
  taskEditorVisible.value = false
  editingTaskIndex.value = -1
  Object.assign(taskDraft, createEmptyTaskDraft())
}

function applyTaskDraft() {
  const nextTask = normalizeTask(taskDraft)
  if (!hasTaskContent(nextTask)) {
    showToast('请至少填写任务进展或时间信息')
    return
  }
  if (editingTaskIndex.value >= 0) {
    taskList.value.splice(editingTaskIndex.value, 1, nextTask)
  } else {
    taskList.value.push(nextTask)
  }
  syncEditorFromTaskList()
  taskEditorVisible.value = false
  editingTaskIndex.value = -1
}

function handleTaskCardClick(index) {
  if (!canEditCurrentWeekly.value) {
    return
  }
  openTaskEditor(index)
}

function closeDetail() {
  clearDetailContext()
}

function handleExitView() {
  if (detailVisible.value) {
    closeDetail()
    showToast('已关闭周报详情')
  }
}

function openReturnSheet(record) {
  if (!canReview(record) || pageBusy.value) {
    return
  }
  returnSheetRecord.value = record
  returnSheetDraft.target = deriveReturnTargets(record)[0] || ''
  returnSheetDraft.comment = ''
  returnSheetVisible.value = true
}

function closeReturnSheet() {
  returnSheetVisible.value = false
  returnSheetRecord.value = null
  returnSheetDraft.target = ''
  returnSheetDraft.comment = ''
}

function showApprovalActions(record) {
  return canReview(record)
}

function handleDetailVisibilityChange(value) {
  if (value) {
    detailVisible.value = true
    return
  }
  closeDetail()
}

async function hydrateRecord(id, sourceItem = null) {
  if (!id) {
    return null
  }
  const response = await queryWeeklyWorkDetail(id)
  const mergedRecord = normalizeWeeklyWorkRecord({
    ...(sourceItem || {}),
    ...(response.data || {})
  })
  upsertRecord(mergedRecord)
  await ensureOrgChainLoaded(resolveRecordUserId(mergedRecord))
  return mergedRecord
}

async function refreshWorkflowState(recordId, options = {}) {
  const { openDetail = detailVisible.value, syncCurrent = true, tabKey = activeRecordTab.value, excludeId = null } = options
  if (!recordId) {
    return null
  }
  logWeeklyWorkFocus('refresh-start', {
    tab: tabKey,
    focusId: recordId,
    transition: focusTransitioning.value ? focusTransitionLabel.value || 'true' : 'false',
    extra: `openDetail=${openDetail}, syncCurrent=${syncCurrent}, excludeId=${excludeId ?? 'null'}`
  })
  beginFocusTransition(tabKey === 'pending' ? '正在切换到下一条待审记录' : '正在同步当前记录')
  if (openDetail) {
    detailVisible.value = true
    detailRecord.value = null
    detailLoading.value = true
  } else {
    clearDetailContext()
  }
  try {
    await fetchListWithOptions({ force: true, focusId: recordId })
    const latestRecord = resolveFocusRecordForTab(tabKey, recordId, { excludeId })
    activeRecordId.value = latestRecord?.id ?? null
    logWeeklyWorkFocus('focus-fallback', {
      tab: tabKey,
      focusId: latestRecord?.id ?? 'null',
      record: latestRecord,
      transition: focusTransitionLabel.value || 'true',
      extra: `sourceId=${recordId}, excludeId=${excludeId ?? 'null'}`
    })
    if (openDetail) {
      if (latestRecord?.id) {
        await openDetailById(latestRecord.id, latestRecord, { syncCurrent, force: true })
        return detailRecord.value
      }
      logWeeklyWorkFocus('focus-empty', {
        tab: tabKey,
        focusId: 'null',
        transition: focusTransitionLabel.value || 'true',
        extra: 'reason=no-focus-record'
      })
      showDetailEmptyState()
      return null
    }
    if (latestRecord && syncCurrent) {
      fillCurrentRecord(latestRecord, canEdit(latestRecord) ? 'edit' : 'view')
    } else if (!latestRecord && isAuditTab.value) {
      clearDetailContext()
    }
    return latestRecord
  } finally {
    if (!detailLoading.value) {
      endFocusTransition()
    }
  }
}

async function openCreateForm(options = {}) {
  if (pageBusy.value) {
    return
  }
  const confirmed = await confirmDiscardIfDirty('切换到新建周报后，当前未保存内容将丢失，确认继续吗？')
  if (!confirmed) {
    return
  }
  const date = options.date || selectedCalendarDate.value || formatDateKey(new Date())
  navigateToEditorPage({ date })
}

async function fetchList() {
  return fetchListWithOptions()
}

async function fetchListWithOptions(options = {}) {
  const { force = false, focusId = null } = options
  if (!force && pageBusy.value && !listLoading.value) {
    return
  }
  listLoading.value = true
  try {
    const response = await queryWeeklyWorkList({})
    list.value = Array.isArray(response.data) ? response.data.map((item) => normalizeWeeklyWorkRecord(item)) : []
    await warmupOrgChains(list.value)
    syncCurrentRecordFromList(
      focusId || routeRecordId.value || activeRecordId.value || editorForm.id || (!isEditorPage.value ? list.value[0]?.id : null)
    )
  } catch (error) {
    showToast(error.message || '周报列表加载失败')
  } finally {
    listLoading.value = false
  }
}

function refreshRecords() {
  fetchListWithOptions({ force: true, focusId: activeRecordId.value || editorForm.id })
}

function navigateToListPage(options = {}) {
  const query = {}
  if (options.tab) {
    query.tab = options.tab
  }
  if (options.focusId) {
    query.focusId = String(options.focusId)
  }
  if (options.date) {
    query.date = options.date
  }
  router.push({ path: '/weekly-work', query })
}

function navigateToEditorPage(options = {}) {
  const query = {}
  if (options.id) {
    query.id = String(options.id)
  }
  if (options.date) {
    query.date = options.date
  }
  router.push({ path: '/weekly-work/editor', query })
}

function shiftCalendarMonth(offset) {
  const nextMonth = new Date(calendarCursor.value)
  nextMonth.setMonth(nextMonth.getMonth() + offset)
  calendarCursor.value = getMonthStart(nextMonth)
}

function jumpCalendarToToday() {
  calendarCursor.value = getMonthStart(new Date())
  selectedCalendarDate.value = formatDateKey(new Date())
}

function selectCalendarDate(day) {
  if (!day?.dateKey) {
    return
  }
  selectedCalendarDate.value = day.dateKey
  if (!day.isCurrentMonth) {
    calendarCursor.value = getMonthStart(day.dateKey)
  }
}

function handleCalendarRecordOpen(item) {
  activeRecordTab.value = 'mine'
  activeRecordId.value = item?.id ?? null
  openDetail(item, { syncCurrent: true })
}

function syncCurrentRecordFromList(targetId) {
  const fallbackRecord = isAuditTab.value
    ? resolveFocusRecordForTab(activeRecordTab.value, targetId)
    : null
  const effectiveTargetId = targetId || (!isEditorPage.value ? fallbackRecord?.id : null)
  if (!effectiveTargetId) {
    if (isAuditTab.value) {
      activeRecordId.value = null
      clearDetailContext()
    }
    return
  }
  const matched = list.value.find((item) => Number(item.id) === Number(effectiveTargetId))
  if (matched) {
    activeRecordId.value = matched.id
    fillCurrentRecord(matched, canEdit(matched) || approvalRevisionMode.value ? 'edit' : 'view')
    if (detailVisible.value && Number(detailRecord.value?.id) === Number(effectiveTargetId)) {
      detailRecord.value = {
        ...(detailRecord.value || {}),
        ...matched
      }
    }
    return
  }
  if (isAuditTab.value) {
    activeRecordId.value = null
    clearDetailContext()
  }
}

function upsertRecord(record) {
  const normalized = normalizeWeeklyWorkRecord(record)
  const index = list.value.findIndex((item) => Number(item.id) === Number(normalized.id))
  if (index >= 0) {
    list.value.splice(index, 1, {
      ...list.value[index],
      ...normalized
    })
    return
  }
  list.value.unshift(normalized)
}

function canEdit(item) {
  return Number(item.userId) === currentUserId.value && (
    item.status === 'DRAFT' ||
    (item.status === 'RETURNED' && isOriginApprovalNode(item))
  )
}

function canSubmit(item) {
  return Number(item.userId) === currentUserId.value && (
    item.status === 'DRAFT' ||
    (item.status === 'RETURNED' && isOriginApprovalNode(item))
  )
}

function showCardEditAction(item) {
  return activeRecordTab.value === 'mine' && canEdit(item)
}

function showCardSubmitAction(item) {
  return activeRecordTab.value === 'mine' && canSubmit(item)
}

function showCardReviewAction(item) {
  return activeRecordTab.value === 'pending' && canReview(item)
}

function showCardDetailAction(item) {
  if (!item) {
    return false
  }
  if (activeRecordTab.value === 'mine') {
    return !showCardEditAction(item) && !showCardSubmitAction(item)
  }
  return activeRecordTab.value === 'processed'
}

function hasApprovalSnapshot(record) {
  if (!record || typeof record !== 'object') {
    return false
  }
  return [
    'currentHandlerUserId',
    'current_handler_user_id',
    'currentFlowOrder',
    'current_flow_order',
    'currentHandlerUserName',
    'current_handler_user_name',
    'approvedTime',
    'approved_time',
    'finalApproverUserId',
    'final_approver_user_id'
  ].some((field) => record[field] !== undefined && record[field] !== null && record[field] !== '')
}

function resolveCurrentHandlerUserId(record) {
  const value = Number(record?.currentHandlerUserId ?? record?.current_handler_user_id ?? 0)
  return Number.isFinite(value) && value > 0 ? value : 0
}

function isWorkflowApproved(record = null) {
  return record?.status === 'APPROVED' || Boolean(record?.approvedTime || record?.approved_time)
}

function resolveCurrentReviewerUserIdByLegacyNode(record) {
  const currentNode = resolveCurrentApprovalNode(record)
  if (String(currentNode).startsWith('USER_')) {
    return Number(String(currentNode).slice(5)) || 0
  }
  const matchedNode = buildFlowNodes(record).find((node) => matchesFlowNode(node, currentNode))
  return Number(matchedNode?.userId || 0) || 0
}

function canReview(item) {
  if (!item) {
    return false
  }
  if (Number(item.userId) === currentUserId.value) {
    return false
  }
  if (isWorkflowApproved(item)) {
    return false
  }
  const currentHandlerUserId = resolveCurrentHandlerUserId(item)
  if (currentHandlerUserId) {
    return currentHandlerUserId === currentUserId.value
  }
  if (hasApprovalSnapshot(item)) {
    return false
  }
  const reviewerUserId = resolveCurrentReviewerUserIdByLegacyNode(item)
  if (!reviewerUserId) {
    return false
  }
  return reviewerUserId === currentUserId.value
}

function canReviseAfterReview(item) {
  return false
}

function isActiveRecord(item) {
  if (isAuditTab.value && !activeRecordId.value && !detailVisible.value) {
    return Number(item?.id) === Number(currentTabRecords.value[0]?.id)
  }
  if (detailVisible.value && Number(detailRecord.value?.id) === Number(item?.id)) {
    return true
  }
  return Number(item?.id) === Number(activeRecordId.value)
}

function resolveCurrentApprovalNode(record) {
  if (isWorkflowApproved(record)) {
    return 'APPROVED'
  }
  if (record?.currentApprovalNode) {
    return record.currentApprovalNode
  }
  if (record?.status === 'SUBMITTED' || record?.status === 'PENDING_SECTION_CHIEF') {
    return 'SECTION_CHIEF'
  }
  if (record?.status === 'PENDING_DEPUTY_LEADER') {
    return 'DEPUTY_LEADER'
  }
  if (record?.status === 'PENDING_LEGION_LEADER') {
    return 'LEGION_LEADER'
  }
  if (record?.status === 'RETURNED') {
    return record?.lastReturnTarget || 'STAFF'
  }
  return 'STAFF'
}

function isFinalApprovalStep(record = null) {
  if (!record || isWorkflowApproved(record)) {
    return false
  }
  const flowNodes = buildFlowNodes(record)
  const activeIndex = findFlowNodeIndex(flowNodes, resolveCurrentApprovalNode(record))
  return activeIndex >= 0 && activeIndex === flowNodes.length - 1
}

function reviewApproveButtonLabel(record = null) {
  return isFinalApprovalStep(record) ? '审批完成' : '提交下一级'
}

function currentStageText(record = null) {
  if (!record) {
    return '-'
  }
  if (isWorkflowApproved(record)) {
    return '已完成'
  }
  const currentNode = resolveCurrentApprovalNode(record)
  const matchedDisplayNode = findDisplayFlowNode(record, currentNode)
  return matchedDisplayNode?.label || approvalNodeLabel(currentNode, record)
}

function nextFlowStepText(record = null) {
  if (!record) {
    return '无下一步'
  }
  if (isWorkflowApproved(record)) {
    return '流程结束'
  }
  if (record.status === 'RETURNED') {
    return `由 ${returnTargetText(record)} 修改后重提`
  }
  if (record.status === 'DRAFT') {
    const flowNodes = buildFlowNodes(record)
    return flowNodes[1]?.label || '提交后自动流转'
  }
  if (isFinalApprovalStep(record)) {
    return '审批完成'
  }
  const flowNodes = buildFlowNodes(record)
  const currentIndex = findFlowNodeIndex(flowNodes, resolveCurrentApprovalNode(record))
  const nextNode = currentIndex >= 0 ? flowNodes[currentIndex + 1] : null
  return nextNode?.label || '审批完成'
}

function returnTargetText(record = null) {
  if (!record || record.status !== 'RETURNED') {
    return '无退回目标'
  }
  const latestReturnLog = findLatestReturnLog(record)
  const targetCode = latestReturnLog?.toNode || record?.lastReturnTarget
  const matchedDisplayNode = findDisplayFlowNode(record, targetCode)
  return matchedDisplayNode?.label || approvalNodeLabel(targetCode, record)
}

function currentFlowTargetText(record = null) {
  const status = record?.status || ''
  const currentNodeLabel = approvalNodeLabel(resolveCurrentApprovalNode(record), record)
  if (isWorkflowApproved(record)) {
    return '流程已完成'
  }
  if (status === 'RETURNED') {
    return `已退回至 ${currentNodeLabel} 待修改`
  }
  if (status === 'DRAFT') {
    return '待本人提交'
  }
  return `${currentNodeLabel} 当前待审批`
}

function flowChainSummary(record = null) {
  const nodes = buildFlowNodes(record || {})
  if (!nodes.length) {
    return '未生成审批链'
  }
  return nodes.map((node) => node.label || approvalNodeLabel(node, record)).join(' -> ')
}

function flowNodeStateLabel(nodeOrState, record = null) {
  const state = typeof nodeOrState === 'object' ? nodeOrState?.state : nodeOrState
  const node = typeof nodeOrState === 'object' ? nodeOrState : null
  const latestReturnLog = findLatestReturnLog(record)
  if (record?.status === 'RETURNED' && node && latestReturnLog && matchesFlowNode(node, latestReturnLog.fromNode)) {
    return '退回发起节点'
  }
  if (state === 'returned') {
    return '已退回至此'
  }
  if (state === 'current') {
    if (record?.status === 'DRAFT') {
      return '待提交'
    }
    return '当前审批'
  }
  if (state === 'done') {
    return isWorkflowApproved(record) ? '已完成' : '已处理'
  }
  if (state === 'skipped') {
    return '已跳过'
  }
  return '待审批'
}

function flowStateSummary(record = null) {
  const nodes = buildFlowNodes(record || {})
  if (!nodes.length) {
    return '未生成审批链'
  }
  return nodes
    .map((node) => `${node.label || approvalNodeLabel(node, record)}（${flowNodeStateLabel(node, record)}）`)
    .join(' -> ')
}

function deriveReturnTargets(record) {
  const flowNodes = buildFlowNodes(record)
  const currentNode = resolveCurrentApprovalNode(record)
  const activeIndex = findFlowNodeIndex(flowNodes, currentNode)
  const previousNodes = activeIndex > 0 ? flowNodes.slice(0, activeIndex) : []
  if (Array.isArray(record?.availableReturnTargets) && record.availableReturnTargets.length) {
    const availableSet = new Set(record.availableReturnTargets)
    const filtered = record.availableReturnTargets.filter((targetCode) => {
      return previousNodes.some((node) => {
        return [node.key, ...(node.aliases || [])].some((candidate) => String(candidate) === String(targetCode))
      })
    })
    if (filtered.length) {
      return filtered
    }
  }
  return previousNodes.map((node) => {
    return (node.aliases || []).find((alias) => LEGACY_FLOW_SEQUENCE.includes(alias)) || node.key
  })
}

async function handleSaveDraft(options = {}) {
  const { silentSuccess = false, successMessage = '周报已暂存', failureMessage = '暂存失败' } = options
  if (!editorForm.weekNo) {
    showToast('请输入周次')
    return null
  }
  syncEditorFromTaskList()
  submitting.value = true
  try {
    const response = await saveWeeklyWorkDraft({
      weekNo: editorForm.weekNo,
      workPlan: editorForm.workPlan,
      workContent: editorForm.workContent,
      remark: editorForm.remark
    })
    const savedId = response.data
    editorForm.id = savedId
    editorMeta.status = 'DRAFT'
    editorMeta.currentApprovalNode = 'STAFF'
    upsertRecord({
      id: savedId,
      userId: currentUserId.value,
      username: userStore.userInfo?.username || '',
      realName: userStore.userInfo?.realName || '',
      weekNo: editorForm.weekNo,
      status: 'DRAFT',
      currentApprovalNode: 'STAFF',
      workPlan: editorForm.workPlan,
      workContent: editorForm.workContent,
      remark: editorForm.remark,
      submitTime: null,
      createTime: new Date().toISOString()
    })
    activeRecordId.value = savedId
    editorMode.value = 'edit'
    await fetchListWithOptions({ force: true, focusId: savedId })
    commitEditorSnapshot()
    if (!silentSuccess) {
      showToast(successMessage)
    }
    return savedId
  } catch (error) {
    showToast(error.message || failureMessage)
    return null
  } finally {
    submitting.value = false
  }
}

async function handleSubmitCurrent() {
  const targetId = await handleSaveDraft({
    silentSuccess: true,
    failureMessage: '保存失败，无法提交到下一步'
  })
  if (!targetId) {
    return
  }
  submitting.value = true
  try {
    await submitWeeklyWork({ id: targetId })
    approvalRevisionMode.value = false
    await refreshWorkflowState(targetId, { openDetail: false, syncCurrent: true, tabKey: activeRecordTab.value })
    commitEditorSnapshot()
    showToast('已保存并提交到下一步')
    if (isEditorPage.value) {
      navigateToListPage({ focusId: targetId, tab: 'mine', date: selectedCalendarDate.value })
    }
  } catch (error) {
    showToast(error.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

async function openEditForm(item) {
  if (!canEdit(item) && !approvalRevisionMode.value) {
    showToast('当前周报不可编辑')
    return
  }
  const confirmed = await confirmDiscardIfDirty('切换到编辑态后，当前未保存内容将丢失，确认继续吗？')
  if (!confirmed) {
    return
  }
  closeDetail()
  navigateToEditorPage({ id: item.id })
}

async function enterApprovalRevisionMode(item) {
  if (!canReviseAfterReview(item) || pageBusy.value) {
    return
  }
  try {
    await showConfirmDialog({
      title: '进入审批修改模式',
      message: '当前周报处于审批修改模式，修改完成后可直接点击底部“提交到下一步”。'
    })
  } catch (error) {
    return
  }
  approvalRevisionMode.value = true
  await openEditForm(item)
  commitEditorSnapshot()
  showToast('已进入审批修改模式')
}

async function openDetailById(id, sourceItem = null, options = {}) {
  const { syncCurrent = true, force = false } = options
  if (!id) {
    return
  }
  if (!force && pageBusy.value && !detailVisible.value) {
    return
  }
  if (!force && detailVisible.value && !detailLoading.value && Number(detailRecord.value?.id) === Number(id)) {
    return
  }
  const isSwitchingRecord = Number(detailRecord.value?.id) !== Number(id)
  logWeeklyWorkFocus('open-detail', {
    focusId: id,
    record: sourceItem,
    transition: isSwitchingRecord || force ? focusTransitionLabel.value || 'true' : 'false',
    extra: `syncCurrent=${syncCurrent}, force=${force}`
  })
  detailVisible.value = true
  if (isSwitchingRecord || force) {
    detailRecord.value = null
  }
  detailLoading.value = true
  try {
    const mergedRecord = await hydrateRecord(id, sourceItem)
    if (!mergedRecord) {
      throw new Error('详情加载失败')
    }
    detailRecord.value = mergedRecord
    if (syncCurrent) {
      activeRecordId.value = mergedRecord.id
      fillCurrentRecord(mergedRecord, canEdit(mergedRecord) || approvalRevisionMode.value ? 'edit' : 'view')
    }
    logWeeklyWorkFocus('detail-loaded', {
      focusId: mergedRecord.id,
      record: mergedRecord,
      transition: focusTransitionLabel.value || 'false',
      extra: `syncCurrent=${syncCurrent}`
    })
  } catch (error) {
    detailVisible.value = false
    showToast(error.message || '详情加载失败')
  } finally {
    detailLoading.value = false
    endFocusTransition()
  }
}

function openDetail(item, options = {}) {
  if (pageBusy.value) {
    return
  }
  const syncCurrent = options.syncCurrent ?? activeRecordTab.value === 'mine'
  openDetailById(item.id, item, { ...options, syncCurrent })
}

async function handleSelectRecord(item) {
  if (pageBusy.value || !item?.id) {
    return
  }
  const confirmed = await confirmDiscardIfDirty('切换记录后，当前未保存修改将丢失，确认继续切换吗？')
  if (!confirmed) {
    return
  }
  activeRecordId.value = item.id
  fillCurrentRecord(item, canEdit(item) ? 'edit' : 'view')
  approvalRevisionMode.value = false
  try {
    const record = await hydrateRecord(item.id, item)
    if (!record) {
      return
    }
    fillCurrentRecord(record, canEdit(record) ? 'edit' : 'view')
    if (detailVisible.value) {
      detailRecord.value = record
    }
  } catch (error) {
    showToast(error.message || '周报详情加载失败')
  }
}

function handleRecordCardClick(item) {
  if (activeRecordTab.value === 'mine') {
    handleSelectRecord(item)
    return
  }
  activeRecordId.value = item?.id ?? null
  openDetail(item, { syncCurrent: false })
}

function handleRecordTabChange(tabKey) {
  if (pageBusy.value || activeRecordTab.value === tabKey) {
    return
  }
  logWeeklyWorkFocus('tab-switch', {
    tab: tabKey,
    focusId: resolveFocusRecordForTab(tabKey)?.id ?? 'null',
    record: resolveFocusRecordForTab(tabKey),
    transition: 'false',
    extra: `fromTab=${activeRecordTab.value}`
  })
  beginFocusTransition(tabKey === 'mine' ? '正在切换填报分区' : '正在切换审核分区')
  const nextFocusRecord = resolveFocusRecordForTab(tabKey)
  activeRecordTab.value = tabKey
  activeRecordId.value = nextFocusRecord?.id ?? null
  if (!nextFocusRecord) {
    if (isAuditTab.value) {
      showDetailEmptyState()
    } else {
      clearDetailContext()
    }
    endFocusTransition()
    return
  }
  if (tabKey === 'mine') {
    endFocusTransition()
    return
  }
  if (detailVisible.value && Number(detailRecord.value?.id) !== Number(nextFocusRecord.id)) {
    detailRecord.value = null
    detailLoading.value = false
  }
  endFocusTransition()
}

async function handleSubmitFromList(item) {
  if (!canSubmit(item)) {
    showToast('当前周报不可提交')
    return
  }
  submitTargetId.value = item.id
  try {
    await showConfirmDialog({
      title: '确认提交',
      message: `确认提交 ${item.weekNo} 的周报吗？`
    })
    await submitWeeklyWork({ id: item.id })
    approvalRevisionMode.value = false
    await refreshWorkflowState(item.id, { openDetail: true, syncCurrent: true, tabKey: activeRecordTab.value })
    commitEditorSnapshot()
    showToast('周报已提交')
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '周报提交失败')
    }
  } finally {
    submitTargetId.value = null
  }
}

async function handleReview(item, action) {
  if (!canReview(item)) {
    showToast('当前周报不可审核')
    return
  }
  logWeeklyWorkFocus(action === 'APPROVE' ? 'review-approve' : 'review-return', {
    focusId: item.id,
    record: item,
    transition: focusTransitioning.value ? focusTransitionLabel.value || 'true' : 'false'
  })
  reviewTargetId.value = item.id
  reviewAction.value = action
  try {
    await showConfirmDialog({
      title: action === 'APPROVE' ? '确认通过' : '确认退回',
      message: `${action === 'APPROVE' ? '通过' : '退回'} ${item.weekNo} 的周报？`
    })
    await reviewWeeklyWork({
      id: item.id,
      action
    })
    await refreshWorkflowState(item.id, {
      openDetail: false,
      syncCurrent: false,
      tabKey: activeRecordTab.value,
      excludeId: action === 'APPROVE' || action === 'RETURN' ? item.id : null
    })
    commitEditorSnapshot()
    showToast(action === 'APPROVE' ? '审核通过' : '已退回')
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || (action === 'APPROVE' ? '提交下一级失败' : '审批提交失败'))
    }
  } finally {
    reviewTargetId.value = null
    reviewAction.value = ''
  }
}

function handleApprove(item) {
  return handleReview(item, 'APPROVE')
}

async function confirmReturnFromSheet() {
  if (!returnSheetRecord.value) {
    return
  }
  if (!returnSheetDraft.target) {
    showToast('请选择退回目标')
    return
  }
  if (!String(returnSheetDraft.comment).trim()) {
    showToast('退回时必须填写退回意见')
    return
  }
  logWeeklyWorkFocus('review-return', {
    focusId: returnSheetRecord.value.id,
    record: returnSheetRecord.value,
    transition: focusTransitioning.value ? focusTransitionLabel.value || 'true' : 'false',
    extra: `target=${returnSheetDraft.target}`
  })
  reviewTargetId.value = returnSheetRecord.value.id
  reviewAction.value = 'RETURN'
  try {
    await showConfirmDialog({
      title: '确认退回',
      message: `确认退回给${approvalNodeLabel(returnSheetDraft.target, returnSheetRecord.value)}吗？`
    })
    await reviewWeeklyWork({
      id: returnSheetRecord.value.id,
      action: 'RETURN',
      returnTarget: returnSheetDraft.target,
      comment: String(returnSheetDraft.comment).trim()
    })
    const targetRecord = returnSheetRecord.value
    closeReturnSheet()
    await refreshWorkflowState(targetRecord.id, {
      openDetail: false,
      syncCurrent: false,
      tabKey: activeRecordTab.value,
      excludeId: targetRecord.id
    })
    commitEditorSnapshot()
    showToast('已退回')
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message)
    }
  } finally {
    reviewTargetId.value = null
    reviewAction.value = ''
  }
}

async function handleExitCurrentMode() {
  if (currentMode.value === 'approval_edit') {
    const confirmed = await confirmDiscardIfDirty('退出审批修改模式后，本次未保存修改将丢失，确认退出吗？')
    if (!confirmed) {
      return
    }
    approvalRevisionMode.value = false
    if (activeRecord.value) {
      fillCurrentRecord(activeRecord.value, 'view')
    } else {
      resetEditor()
    }
    showToast('已退出审批修改模式')
    return
  }
  if (currentMode.value === 'edit' && activeRecord.value) {
    const confirmed = await confirmDiscardIfDirty('退出编辑后，当前未保存修改将丢失，确认退出吗？')
    if (!confirmed) {
      return
    }
    fillCurrentRecord(activeRecord.value, 'view')
    showToast('已退出编辑')
  }
}

async function syncPageStateFromRoute() {
  const requestedTab = ['mine', 'pending', 'processed'].includes(String(route.query.tab || ''))
    ? String(route.query.tab)
    : 'mine'
  const routeTab = !showAuditRecordTabs.value && requestedTab !== 'mine' ? 'mine' : requestedTab
  const routeFocusId = Number(route.query.focusId || 0) || null
  const targetDate = routeDraftDate.value || formatDateKey(new Date())

  activeRecordTab.value = routeTab
  selectedCalendarDate.value = targetDate
  calendarCursor.value = getMonthStart(targetDate)

  if (!isEditorPage.value) {
    approvalRevisionMode.value = false
    if (!routeRecordId.value) {
      if (!editorForm.id || routeTab !== 'mine') {
        resetEditor()
      }
      if (routeFocusId) {
        activeRecordId.value = routeFocusId
      }
    }
    return
  }

  closeDetail()
  if (routeRecordId.value) {
    activeRecordId.value = routeRecordId.value
    const matched = list.value.find((item) => Number(item.id) === Number(routeRecordId.value))
    if (matched) {
      fillCurrentRecord(matched, 'edit')
    }
    try {
      const record = await hydrateRecord(routeRecordId.value, matched || null)
      if (record) {
        fillCurrentRecord(record, 'edit')
      }
    } catch (error) {
      showToast(error.message || '周报详情加载失败')
    }
    return
  }

  resetEditor()
  editorForm.weekNo = getWeekNoFromDate(targetDate)
  commitEditorSnapshot()
}

onMounted(() => {
  syncTaskListFromEditor()
  commitEditorSnapshot()
  ensureOrgChainLoaded(currentUserId.value)
  fetchCurrentUserOrgChildren()
  syncPageStateFromRoute()
  fetchList()
})

watch(
  () => route.fullPath,
  () => {
    syncPageStateFromRoute()
  }
)

watch(showAuditRecordTabs, (visible) => {
  if (!visible && activeRecordTab.value !== 'mine') {
    activeRecordTab.value = 'mine'
    clearDetailContext()
  }
})
</script>

<style scoped>
.action-row,
.panel-actions,
.card-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.action-row {
  width: 100%;
  justify-content: flex-end;
}

.panel {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
}

.status-hero,
.task-board {
  margin-bottom: 16px;
}

.status-hero {
  padding: 16px;
  border-radius: 20px;
  background:
    linear-gradient(160deg, rgba(30, 41, 59, 0.96), rgba(51, 65, 85, 0.88)),
    linear-gradient(180deg, #f8fafc, #eef2ff);
  color: #f8fafc;
  box-shadow: 0 18px 42px rgba(15, 23, 42, 0.18);
}

.status-hero--audit {
  background:
    linear-gradient(160deg, rgba(15, 118, 110, 0.96), rgba(15, 23, 42, 0.92)),
    linear-gradient(180deg, #f0fdf4, #ecfeff);
}

.status-hero__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.status-hero__eyebrow {
  font-size: 12px;
  letter-spacing: 0.08em;
  color: rgba(236, 254, 255, 0.82);
}

.status-hero__title {
  margin-top: 6px;
  font-size: 22px;
  font-weight: 700;
  line-height: 1.3;
}

.status-hero__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 12px;
  font-size: 13px;
  color: rgba(236, 254, 255, 0.92);
}

.status-hero__alert {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.14);
  font-size: 13px;
  line-height: 1.6;
  color: #fff7ed;
}

.status-hero__notice {
  margin-top: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(255, 255, 255, 0.16);
  font-size: 13px;
  line-height: 1.6;
  color: #e0f2fe;
}

.status-hero__focus-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.status-hero__focus-card {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.12);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.12);
}

.status-hero__focus-card--current {
  background: rgba(255, 255, 255, 0.22);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.18), 0 10px 22px rgba(15, 23, 42, 0.12);
}

.status-hero__focus-card--next {
  background: rgba(191, 219, 254, 0.2);
}

.status-hero__focus-card--return {
  background: rgba(251, 191, 36, 0.2);
}

.status-hero__focus-label {
  display: block;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.84);
}

.status-hero__focus-value {
  display: block;
  margin-top: 6px;
  font-size: 15px;
  line-height: 1.5;
  color: #fff;
}

.approval-stepper {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(84px, 1fr);
  gap: 8px;
  margin-top: 16px;
  overflow-x: auto;
  padding-bottom: 2px;
}

.approval-step {
  padding: 10px 8px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.1);
  text-align: center;
}

.approval-step__dot {
  width: 28px;
  height: 28px;
  margin: 0 auto 6px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.approval-step__label {
  font-size: 12px;
  line-height: 1.4;
}

.approval-step__state {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.4;
  color: rgba(226, 232, 240, 0.88);
}

.approval-step--done .approval-step__dot {
  background: #fef3c7;
  color: #92400e;
}

.approval-step--current {
  background: rgba(255, 255, 255, 0.18);
  box-shadow: inset 0 0 0 1px rgba(255, 255, 255, 0.22);
}

.approval-step--current .approval-step__dot {
  background: #fff;
  color: #334155;
}

.approval-step--pending .approval-step__dot {
  background: rgba(255, 255, 255, 0.16);
  color: rgba(236, 254, 255, 0.9);
}

.approval-step--returned {
  background: rgba(251, 191, 36, 0.16);
  box-shadow: inset 0 0 0 1px rgba(251, 191, 36, 0.22);
}

.approval-step--returned .approval-step__dot {
  background: #fff7ed;
  color: #c2410c;
}

.task-board {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.audit-mode-board {
  margin-bottom: 16px;
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.audit-mode-board__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.audit-mode-card {
  margin-top: 14px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid #dbe3ef;
  background: linear-gradient(180deg, #f8fafc, #ffffff);
}

.audit-mode-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.audit-mode-card__eyebrow {
  font-size: 12px;
  color: #64748b;
}

.audit-mode-card__title {
  margin-top: 4px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.audit-mode-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.audit-mode-kv {
  padding: 12px;
  border-radius: 14px;
  border: 1px solid #e5e7eb;
  background: #fff;
}

.audit-mode-kv__label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.audit-mode-kv__value {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
  line-height: 1.5;
}

.audit-mode-card__hint,
.audit-mode-empty__desc {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.audit-mode-empty {
  margin-top: 14px;
  padding: 16px;
  border-radius: 18px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
}

.audit-mode-empty__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.record-board {
  padding: 16px;
  border-radius: 20px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.record-board__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.flow-state-legend {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin: 12px 0;
}

.flow-state-legend--detail {
  margin-top: 14px;
}

.flow-state-legend__item {
  display: inline-flex;
  align-items: center;
  min-height: 26px;
  padding: 0 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 600;
  border: 1px solid #cbd5e1;
  color: #475569;
  background: #f8fafc;
}

.flow-state-legend__item--done {
  border-color: #f59e0b;
  color: #92400e;
  background: #fffbeb;
}

.flow-state-legend__item--current {
  border-color: #60a5fa;
  color: #1d4ed8;
  background: #eff6ff;
}

.flow-state-legend__item--pending {
  border-color: #cbd5e1;
  color: #475569;
  background: #f8fafc;
}

.flow-state-legend__item--returned {
  border-color: #fb923c;
  color: #c2410c;
  background: #fff7ed;
}

.flow-state-legend__item--completed {
  border-color: #34d399;
  color: #047857;
  background: #ecfdf5;
}

.record-tabs {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
  margin-bottom: 14px;
}

.record-focus-banner {
  margin-bottom: 14px;
  padding: 12px 14px;
  border-radius: 16px;
  border: 1px solid #dbe3ef;
  background: linear-gradient(180deg, #f8fafc, #ffffff);
}

.record-focus-banner--audit {
  border-color: #bfdbfe;
  background: linear-gradient(180deg, #eff6ff, #f8fbff);
}

.record-focus-banner--transition {
  border-style: dashed;
  box-shadow: inset 0 0 0 1px rgba(96, 165, 250, 0.08);
}

.record-focus-banner__title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.record-focus-banner__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.record-tab {
  min-height: 52px;
  padding: 10px 14px;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.88);
  color: #334155;
  display: flex;
  flex-direction: row;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: 600;
  text-align: left;
}

.record-tab--active {
  border-color: #1d4ed8;
  background: linear-gradient(180deg, #eff6ff, #dbeafe);
  color: #1e3a8a;
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.14);
}

.record-tab__count {
  font-size: 12px;
  color: #64748b;
  padding: 2px 8px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.72);
}

.record-board__content,
.record-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.record-group__title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.task-board__header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.task-board__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
}

.task-board__summary {
  font-size: 13px;
  color: #64748b;
  line-height: 1.6;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-empty-card {
  padding: 18px 16px;
  border-radius: 18px;
  border: 1px dashed #cbd5e1;
  background: #f8fafc;
}

.task-empty-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.task-empty-card__desc {
  margin: 6px 0 14px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.task-card {
  padding: 16px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.task-card--editable {
  cursor: pointer;
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.task-card--editable:hover {
  transform: translateY(-1px);
  border-color: #bfdbfe;
  box-shadow: 0 14px 28px rgba(37, 99, 235, 0.08);
}

.task-card__header,
.task-card__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.task-card__title {
  font-size: 16px;
  font-weight: 700;
  color: #0f172a;
}

.task-card__subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.task-pill {
  padding: 8px 12px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
}

.task-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.task-kv {
  padding: 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.task-kv__label,
.task-card__label {
  font-size: 12px;
  color: #64748b;
}

.task-kv__value {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  color: #0f172a;
  font-weight: 600;
}

.task-card__block {
  margin-top: 12px;
  padding: 14px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.task-card__content {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  color: #334155;
  white-space: pre-wrap;
  word-break: break-word;
}

.task-card__footer {
  margin-top: 14px;
}

.task-card__hint {
  font-size: 12px;
  color: #94a3b8;
}

.task-card__delete {
  border: 0;
  padding: 0;
  background: transparent;
  color: #dc2626;
  font-size: 13px;
  font-weight: 600;
}

.task-card__delete:disabled {
  color: #cbd5e1;
}

.status-hero__summary {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 12px;
  margin-top: 14px;
  font-size: 12px;
  color: rgba(226, 232, 240, 0.92);
}

.task-editor-sheet {
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  background: #fff;
  border-radius: 24px 24px 0 0;
}

.task-editor-sheet__title {
  margin-bottom: 14px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.task-editor-sheet__section {
  margin-bottom: 14px;
}

.task-editor-sheet__label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.task-editor-sheet__grid {
  display: grid;
  gap: 8px;
}

.nature-pill-group {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.nature-pill {
  min-height: 48px;
  padding: 0 12px;
  border: 1px solid #cbd5e1;
  border-radius: 16px;
  background: #f8fafc;
  font-size: 15px;
  font-weight: 600;
  color: #334155;
}

.nature-pill--active {
  border-color: #1d4ed8;
  background: #eff6ff;
  color: #1e3a8a;
  box-shadow: 0 10px 22px rgba(37, 99, 235, 0.14);
}

.task-editor-sheet__actions {
  display: flex;
  gap: 10px;
  margin-top: 16px;
}

.task-editor-sheet__actions :deep(.van-button) {
  flex: 1;
}

.editor-action-panel {
  margin-top: 16px;
  padding: 14px;
  border-radius: 18px;
  border: 1px solid #dbe3ef;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
}

.editor-action-panel__title {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.editor-action-panel__desc {
  margin-top: 6px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.editor-action-panel__buttons {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.calendar-board__toolbar {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 10px;
  align-items: center;
  margin-top: 14px;
}

.calendar-nav-button {
  min-height: 36px;
  padding: 0 12px;
  border: 1px solid #dbe3ef;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.88);
  color: #475569;
  font-size: 12px;
  font-weight: 600;
}

.calendar-nav-button--today {
  color: #1d4ed8;
}

.panel-title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.card-actions {
  justify-content: flex-end;
}

.panel-hint,
.empty-tip,
.report-meta,
.report-preview,
.detail-item,
.detail-value {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.report-stage-strip {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin: 10px 0 8px;
}

.report-stage-chip {
  padding: 10px;
  border-radius: 14px;
  border: 1px solid #dbe3ef;
  background: #f8fafc;
}

.report-stage-chip--current {
  border-color: #93c5fd;
  background: #eff6ff;
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.12);
}

.report-stage-chip--next {
  border-color: #bfdbfe;
  background: #f8fbff;
}

.report-stage-chip--return {
  border-color: #fdba74;
  background: #fff7ed;
}

.report-stage-chip__label {
  display: block;
  font-size: 11px;
  color: #64748b;
}

.report-stage-chip__value {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.5;
  color: #0f172a;
}

.report-meta--return {
  color: #b45309;
}

.query-grid {
  display: grid;
  gap: 10px;
  margin-bottom: 10px;
}

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border: 1px solid #ebedf0;
  border-radius: 8px;
}

.select-label {
  flex: 0 0 56px;
  font-size: 14px;
  color: #323233;
}

.select-field select {
  flex: 1;
  border: 0;
  background: transparent;
  font-size: 14px;
  color: #323233;
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.report-card-wrap {
  position: relative;
  border-radius: 18px;
  transition: transform 0.2s ease, box-shadow 0.2s ease, border-color 0.2s ease;
}

.report-card-wrap--active {
  transform: translateY(-1px);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.24), 0 12px 28px rgba(37, 99, 235, 0.12);
}

.report-card {
  overflow: hidden;
  border-radius: 18px;
}

.report-card-wrap--active .report-card {
  background: linear-gradient(180deg, #ffffff, #f8fbff);
}

.report-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.report-title__main {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.report-active-badge {
  display: inline-flex;
  align-items: center;
  min-height: 22px;
  padding: 0 8px;
  border-radius: 999px;
  background: #dbeafe;
  color: #1d4ed8;
  font-size: 11px;
  font-weight: 700;
}

.report-preview {
  margin-top: 4px;
}

.task-text-block {
  white-space: pre-wrap;
  word-break: break-word;
}

.report-flow-stepper {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(86px, 1fr);
  gap: 8px;
  margin: 10px 0 6px;
  overflow-x: auto;
  padding-bottom: 2px;
  scroll-snap-type: x proximity;
  -webkit-overflow-scrolling: touch;
}

.approval-step--compact {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  scroll-snap-align: start;
}

.approval-step--compact .approval-step__label {
  color: #0f172a;
}

.approval-step--compact .approval-step__state {
  color: #64748b;
}

.approval-step--compact.approval-step--current {
  background: #eff6ff;
  border-color: #93c5fd;
  box-shadow: 0 10px 24px rgba(59, 130, 246, 0.14);
}

.approval-step--compact.approval-step--done {
  background: #f8fafc;
}

.approval-step--compact.approval-step--returned {
  background: #fff7ed;
  border-color: #fdba74;
}

.state-block {
  padding: 20px 0;
}

.detail-panel {
  padding: 16px;
}

.detail-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 12px;
}

.detail-title {
  font-size: 16px;
  font-weight: 600;
}

.detail-loading-card {
  padding: 20px 16px calc(20px + env(safe-area-inset-bottom));
  text-align: center;
}

.detail-loading-card__desc {
  margin-top: 10px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.detail-empty-card {
  padding: 24px 16px calc(24px + env(safe-area-inset-bottom));
  text-align: center;
  color: #475569;
}

.detail-empty-card__title {
  font-size: 15px;
  font-weight: 700;
  color: #0f172a;
}

.detail-empty-card__desc {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.7;
  color: #64748b;
}

.detail-summary-card {
  padding: 14px;
  border-radius: 18px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  border: 1px solid #e2e8f0;
}

.detail-summary-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.detail-summary-card__eyebrow {
  font-size: 12px;
  color: #64748b;
}

.detail-summary-card__title {
  margin-top: 4px;
  font-size: 20px;
  font-weight: 700;
  color: #0f172a;
}

.detail-summary-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 14px;
}

.detail-summary-kv {
  padding: 12px;
  border-radius: 14px;
  background: #fff;
  border: 1px solid #e5e7eb;
}

.detail-summary-kv__label {
  display: block;
  font-size: 12px;
  color: #64748b;
}

.detail-summary-kv__value {
  display: block;
  margin-top: 6px;
  font-size: 14px;
  font-weight: 600;
  color: #0f172a;
}

.detail-summary-card__return {
  margin-top: 12px;
  padding: 10px 12px;
  border-radius: 14px;
  background: #fff7ed;
  color: #9a3412;
  font-size: 13px;
  line-height: 1.6;
}

.detail-summary-card__chain {
  margin-top: 12px;
  font-size: 13px;
  line-height: 1.7;
  color: #475569;
}

.detail-flow-stepper {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: minmax(84px, 1fr);
  gap: 8px;
  margin-top: 14px;
  overflow-x: auto;
  padding-bottom: 2px;
  scroll-snap-type: x proximity;
  -webkit-overflow-scrolling: touch;
}

.detail-block {
  margin-top: 12px;
}

.detail-label {
  margin-bottom: 4px;
  font-size: 13px;
  color: #111827;
  font-weight: 600;
}

.detail-task-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.detail-task-card {
  padding: 12px;
  border-radius: 16px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.detail-task-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  font-size: 13px;
  font-weight: 700;
  color: #0f172a;
}

.detail-task-card__meta,
.detail-task-card__block {
  margin-top: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #475569;
}

.approval-action-card {
  padding: 14px;
  border-radius: 18px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.approval-action-card__hint {
  margin-top: 4px;
  font-size: 13px;
  color: #64748b;
}

.approval-action-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.approval-action-grid__danger {
  opacity: 0.92;
}

.approval-timeline {
  padding: 14px;
  border-radius: 18px;
  background: #fbfdff;
  border: 1px solid #e5eef8;
}

.timeline-item {
  margin-top: 10px;
  padding: 0 0 0 14px;
  border-left: 2px solid #dbeafe;
}

.timeline-item__head {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 8px;
}

.timeline-title {
  margin-top: 8px;
  font-size: 13px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.5;
}

.timeline-item__time,
.timeline-detail {
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.timeline-detail {
  margin-top: 6px;
}

.timeline-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 54px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 700;
}

.timeline-tag--submit {
  background: #dbeafe;
  color: #1d4ed8;
}

.timeline-tag--approve {
  background: #dcfce7;
  color: #166534;
}

.timeline-tag--return {
  background: #fee2e2;
  color: #b91c1c;
}

.timeline-tag--modify {
  background: #ede9fe;
  color: #6d28d9;
}

.timeline-detail--return {
  color: #b45309;
}

.return-sheet {
  padding: 18px 16px calc(18px + env(safe-area-inset-bottom));
  background: #fff;
  border-radius: 24px 24px 0 0;
}

.return-sheet__title {
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.return-sheet__subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #64748b;
}

.return-sheet__section {
  margin: 16px 0 14px;
}

.return-sheet__label {
  margin-bottom: 10px;
  font-size: 13px;
  font-weight: 600;
  color: #334155;
}

.return-target-list {
  display: grid;
  gap: 10px;
}

.return-target-card {
  width: 100%;
  padding: 14px;
  text-align: left;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: #f8fafc;
}

.return-target-card--active {
  border-color: #1d4ed8;
  background: #eff6ff;
  box-shadow: 0 10px 24px rgba(37, 99, 235, 0.12);
}

.return-target-card__title {
  display: block;
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.return-target-card__desc {
  display: block;
  margin-top: 4px;
  font-size: 12px;
  color: #64748b;
}

.return-sheet__actions {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
  margin-top: 16px;
}

.calendar-board {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 20px;
  background: linear-gradient(180deg, #fffdf7, #ffffff);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.calendar-board__header,
.calendar-board__footer {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.calendar-board__actions,
.editor-action-panel__buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.calendar-board__month {
  margin-top: 14px;
  font-size: 18px;
  font-weight: 700;
  color: #0f172a;
}

.calendar-grid {
  display: grid;
  gap: 6px;
  margin-top: 12px;
}

.calendar-weekdays,
.calendar-week-row {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  gap: 6px;
}

.calendar-grid__weekday {
  text-align: center;
  font-size: 12px;
  font-weight: 700;
  color: #64748b;
}

.calendar-day {
  min-height: 76px;
  padding: 7px;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  background: #fff;
  text-align: left;
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 6px;
  color: #0f172a;
  overflow: hidden;
}

.calendar-day--muted {
  opacity: 0.48;
}

.calendar-day--today {
  border-color: #fb923c;
  box-shadow: inset 0 0 0 1px rgba(249, 115, 22, 0.16);
}

.calendar-day--selected {
  border-color: #2563eb;
  background: linear-gradient(180deg, #eff6ff, #ffffff);
  box-shadow: 0 12px 28px rgba(37, 99, 235, 0.12);
}

.calendar-day__top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 6px;
}

.calendar-day__date {
  font-size: 14px;
  font-weight: 700;
}

.calendar-day__badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 999px;
  background: #eff6ff;
  color: #1d4ed8;
  font-size: 10px;
  font-weight: 700;
}

.calendar-day__events {
  display: flex;
  flex-direction: column;
  gap: 3px;
  min-height: 0;
}

.calendar-day__event,
.calendar-day__more {
  display: block;
  padding: 2px 6px;
  border-radius: 10px;
  font-size: 9px;
  line-height: 1.35;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.calendar-day__event {
  background: #f8fafc;
  color: #475569;
}

.calendar-day__more {
  color: #94a3b8;
}

.calendar-board__footer {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e2e8f0;
}

.calendar-board__focus {
  font-size: 14px;
  font-weight: 700;
  color: #0f172a;
}

.calendar-board__hint {
  margin-top: 4px;
  font-size: 12px;
  line-height: 1.6;
  color: #64748b;
}

.calendar-record-list {
  display: grid;
  gap: 8px;
  margin-top: 10px;
}

.calendar-record-card {
  width: 100%;
  padding: 12px;
  border: 1px solid #dbe3ef;
  border-radius: 16px;
  background: linear-gradient(180deg, #ffffff, #f8fafc);
  text-align: left;
}

.calendar-record-card__top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.calendar-record-card__meta {
  margin-top: 6px;
  font-size: 11px;
  line-height: 1.5;
  color: #64748b;
}

.audit-mode-card--placeholder {
  border-style: dashed;
}

.approval-stepper {
  scroll-snap-type: x proximity;
  -webkit-overflow-scrolling: touch;
}

.approval-step {
  scroll-snap-align: start;
}

@media (min-width: 768px) {
  .task-editor-sheet__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .approval-action-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .return-target-list {
    grid-template-columns: repeat(3, minmax(0, 1fr));
  }

  .calendar-record-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .record-tabs {
    grid-template-columns: 1fr;
  }

  .record-tab {
    min-height: 50px;
    flex-direction: row;
    justify-content: space-between;
    align-items: center;
  }

  .status-hero__meta,
  .status-hero__summary {
    gap: 6px 10px;
  }

  .record-focus-banner {
    padding: 10px 12px;
  }

  .record-focus-banner__title {
    font-size: 13px;
  }

  .record-focus-banner__desc,
  .audit-mode-card__hint,
  .audit-mode-empty__desc,
  .detail-empty-card__desc {
    font-size: 12px;
  }

  .approval-stepper,
  .report-flow-stepper,
  .detail-flow-stepper {
    grid-auto-columns: minmax(110px, 1fr);
    padding-bottom: 6px;
  }

  .approval-step,
  .approval-step--compact {
    padding: 8px 6px;
  }

  .approval-step__label,
  .approval-step__state {
    word-break: break-word;
  }

  .detail-loading-card__desc,
  .record-focus-banner__desc {
    line-height: 1.7;
  }

  .status-hero__focus-strip,
  .report-stage-strip,
  .audit-mode-card__grid,
  .detail-summary-grid {
    grid-template-columns: 1fr;
  }

  .calendar-board__header,
  .calendar-board__footer {
    flex-direction: column;
  }

  .calendar-day {
    min-height: 74px;
    padding: 6px;
    border-radius: 14px;
  }

  .calendar-weekdays,
  .calendar-week-row {
    gap: 4px;
  }

  .calendar-day__event:nth-child(n + 2) {
    display: none;
  }

  .calendar-day__badge {
    min-width: 16px;
    height: 16px;
    font-size: 9px;
  }
}
</style>
