<template>
  <AppPageShell title="考勤管理" description="当前页已接入最小可用考勤工作流：列表、查询、打卡、补录编辑和删除。">
    <template #title-extra>
      <PageHelp page-key="attendance" />
    </template>
    <template #actions>
      <div v-if="hasSubordinates" class="action-row" data-guide="attendance-checkin">
        <van-button type="primary" :loading="state.checkingIn" :disabled="pageBusy || !personalCanCheckIn" @click="handleCheckIn">{{ personalCheckInButtonText }}</van-button>
        <van-button plain type="success" :loading="state.exporting" :disabled="pageBusy || !state.list.length" @click="handleExport">导出记录</van-button>
        <van-button plain type="primary" :loading="state.loading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
      </div>
    </template>

    <PersonalAttendanceWorkspace
      v-if="!hasSubordinates"
      :loading="personalWorkspaceLoading"
      :name="currentWorkspaceUserName"
      :today-status-label="personalWorkspaceStatus.label"
      :today-status-tone="personalWorkspaceStatus.tone"
      :notice="personalWorkspaceNotice"
      :check-in-time="personalWorkspaceTodayCard.checkInTime"
      :check-out-time="personalWorkspaceTodayCard.checkOutTime"
      :location-text="personalWorkspaceTodayCard.locationText"
      :location-detail="personalWorkspaceTodayCard.locationDetail"
      :location-status="personalWorkspaceTodayCard.locationStatus"
      :can-check-in="personalCanCheckIn"
      :checking-in="state.checkingIn"
      :weekly-stats="personalWorkspaceWeekStats"
      :recent-records="personalRecentRecords"
      :check-in-hint="personalWorkspaceCheckInHint"
      :check-in-button-text="personalCheckInButtonText"
      @check-in="handleCheckIn"
    />

    <template v-else>
    <section class="attendance-leadership-section">
      <AttendanceLeadershipSummarySection
        :loading="state.leadershipLoading"
        :scope-description="state.leadershipScopeDescription"
        :range-text="leadershipWorkweekRangeText"
        :overview="leadershipOverview"
        :alerts="leadershipAlerts"
        :non-workday-notice="leadershipNonWorkdayNotice"
      />

      <div class="attendance-leadership-workspace">
        <div class="attendance-leadership-main">
          <AttendanceLeadershipMemberCards
            :members="leadershipFilteredMembers"
            :loading="state.leadershipLoading"
            :selected-user-id="state.leadershipSelectedUserId"
            :keyword="state.leadershipFilters.keyword"
            :status="state.leadershipFilters.status"
            :department="state.leadershipFilters.department"
            :department-options="leadershipDepartmentOptions"
            :status-options="LEADERSHIP_STATUS_FILTER_OPTIONS"
            @select-member="handleSelectLeadershipMember"
            @update:keyword="handleLeadershipKeywordChange"
            @update:status="handleLeadershipStatusChange"
            @update:department="handleLeadershipDepartmentChange"
            @clear-filters="handleResetLeadershipFilters"
            @refresh="fetchLeadershipWorkspace"
          >
            <template #detail>
              <AttendanceLeadershipDetailPanel
                class="attendance-leadership-detail-mobile"
                :member="selectedLeadershipMember"
                :detail="selectedLeadershipDetail"
                :today-label="leadershipTodayText"
                :week-range-text="leadershipWorkweekRangeText"
                :loading="state.leadershipDetailLoading"
              />
            </template>
          </AttendanceLeadershipMemberCards>
        </div>

        <aside class="attendance-leadership-aside">
          <AttendanceLeadershipDetailPanel
            :member="selectedLeadershipMember"
            :detail="selectedLeadershipDetail"
            :today-label="leadershipTodayText"
            :week-range-text="leadershipWorkweekRangeText"
            :loading="state.leadershipDetailLoading"
          />
        </aside>
      </div>
    </section>

    <details class="attendance-legacy-tools">
      <summary class="attendance-legacy-tools__summary">
        <div>
          <strong>高级分析工具</strong>
          <span>保留旧查询、异常分析与趋势排查，默认折叠以突出工作台主线。</span>
        </div>
      </summary>

    <section class="panel" data-guide="attendance-query">
      <div class="panel-title">查询区</div>
      <div class="query-grid">
        <van-field v-model.trim="state.queryForm.keywords" label="姓名/账号" placeholder="请输入姓名或账号" :disabled="pageBusy" />
        <van-field v-model.trim="state.queryForm.unitName" label="组织名称" placeholder="请输入组织名称" :disabled="pageBusy" />
        <div class="select-field">
          <span class="select-label">结果状态</span>
          <select v-model="state.queryForm.checkInStatus" :disabled="pageBusy" @change="fetchList">
            <option v-for="option in ATTENDANCE_STATUS_OPTIONS" :key="option.value || 'all'" :value="option.value">
              {{ option.label }}
            </option>
          </select>
        </div>
        <van-field v-model="state.queryForm.dateFrom" label="开始日期" type="date" :disabled="pageBusy" />
        <van-field v-model="state.queryForm.dateTo" label="结束日期" type="date" :disabled="pageBusy" />
      </div>
      <div class="quick-range-row">
        <span class="quick-range-label">快捷时间</span>
        <button
          v-for="option in QUICK_RANGE_OPTIONS"
          :key="option.key"
          type="button"
          class="quick-range-button"
          :class="{ 'quick-range-button-active': isQuickRangeActive(option.key) }"
          :disabled="pageBusy"
          @click="handleApplyQuickRange(option.key)"
        >
          {{ option.label }}
        </button>
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.loading" :disabled="pageBusy" @click="handleSearch">查询</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="handleReset">重置</van-button>
      </div>
      <div class="query-context-card">
        <div class="summary-label">当前查询工作面</div>
        <div class="attendance-meta">快捷时间：{{ activeQuickRangeLabel }}</div>
        <div class="attendance-meta">时间范围：{{ listRangeText }}</div>
        <div class="attendance-meta">筛选条件：{{ listFilterSummary || '未附加关键词/组织/异常筛选' }}</div>
        <div class="attendance-meta">联动状态：{{ listLinkageSummary || '无异常用户/趋势日期联动' }}</div>
        <div v-if="state.abnormalSelection.activeUserId || state.trendDateLinkage.selectedDate" class="panel-actions">
          <van-button size="small" plain type="primary" :disabled="pageBusy" @click="handleClearAbnormalSelection">
            清除当前联动
          </van-button>
        </div>
        <div v-if="hasResettableQueryConditions" class="panel-actions">
          <van-button size="small" plain :disabled="pageBusy" @click="handleReset">
            清空当前查询条件
          </van-button>
        </div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">打卡统计</div>
      <div class="panel-hint">统计口径与当前筛选条件、当前数据权限范围保持一致。</div>
      <van-loading v-if="state.summaryLoading" class="state-block" size="24px" vertical>统计加载中...</van-loading>
      <template v-else>
        <div class="summary-grid">
          <div class="summary-card">
            <div class="summary-label">总记录数</div>
            <div class="summary-value">{{ state.summary.totalCount }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">成功数</div>
            <div class="summary-value summary-value-success">{{ state.summary.successCount }}</div>
          </div>
          <div class="summary-card summary-card-danger">
            <div class="summary-label">异常数</div>
            <div class="summary-value summary-value-danger">{{ state.summary.abnormalCount }}</div>
          </div>
          <div class="summary-card">
            <div class="summary-label">异常率</div>
            <div class="summary-value">{{ abnormalRate }}</div>
          </div>
        </div>
        <div v-if="state.summary.statusCounts.length" class="status-summary-list">
          <div v-for="item in state.summary.statusCounts" :key="item.checkInStatus || 'UNKNOWN'" class="status-summary-item">
            <span class="attendance-meta">{{ getAttendanceStatusLabel(item.checkInStatus) }}</span>
            <van-tag size="small" :type="getAttendanceStatusTagType(item.checkInStatus)">
              {{ item.count }}
            </van-tag>
          </div>
        </div>
      </template>
    </section>

    <section class="panel" data-guide="attendance-abnormal">
      <div class="panel-title">异常用户 Top10</div>
      <div class="panel-hint">按当前时间范围和数据权限统计异常次数与异常率，点击用户可直接回筛异常记录。</div>
      <div class="attendance-meta">当前统计范围：{{ abnormalMonitorRangeText }}</div>
      <div class="attendance-meta">异常口径：`check_in_result` 不属于 `CHECK_IN_SUCCESS / CHECK_OUT_SUCCESS`。</div>
      <div class="attendance-meta">高风险人数：{{ state.abnormalMonitor.highRiskCount || 0 }}，预警触发人数：{{ state.abnormalMonitor.alertCount || 0 }}</div>
      <div v-if="state.abnormalSelection.activeUserId" class="attendance-meta">
        当前选中异常用户：{{ selectedAbnormalUserName }}
      </div>
      <div class="panel-actions">
        <van-button size="small" plain type="success" :loading="state.abnormalExporting" :disabled="pageBusy || !state.abnormalMonitor.topUsers.length" @click="handleExportAbnormalRanks">
          导出异常榜单
        </van-button>
        <van-button
          v-if="state.abnormalSelection.activeUserId"
          size="small"
          plain
          type="primary"
          :disabled="pageBusy"
          @click="handleClearAbnormalSelection"
        >
          清除异常联动
        </van-button>
      </div>
      <van-loading v-if="state.abnormalLoading" class="state-block" size="24px" vertical>异常榜单加载中...</van-loading>
      <template v-else>
        <van-empty v-if="!state.abnormalMonitor.topUsers.length" description="当前范围暂无异常用户" />
        <div v-else class="rank-list">
          <button
            v-for="(item, index) in state.abnormalMonitor.topUsers"
            :key="item.userId"
            type="button"
            class="rank-item"
            :class="{ 'rank-item-active': state.abnormalSelection.activeUserId === item.userId }"
            @click="handleSelectAbnormalUser(item)"
          >
            <div class="rank-order">#{{ index + 1 }}</div>
            <div class="rank-body">
              <div class="rank-title">{{ item.realName || item.username || `用户${item.userId}` }}</div>
              <div class="attendance-meta">账号：{{ item.username || '-' }}</div>
              <div class="attendance-meta">组织：{{ item.unitName || '-' }}</div>
              <div class="attendance-meta">主要异常：{{ item.mainReasonLabel || '-' }} / {{ item.mainReasonTag || '-' }}</div>
              <div class="attendance-meta">近7天趋势：{{ trendDirectionLabel(item.trendDirection) }}（{{ item.recent7DayAbnormalCount ?? 0 }} vs {{ item.previous7DayAbnormalCount ?? 0 }}）</div>
              <div v-if="item.alertRuleText" class="attendance-meta">预警规则：{{ item.alertRuleText }}</div>
            </div>
            <div class="rank-stats">
              <div class="rank-badge rank-badge-danger">异常 {{ item.abnormalCount }}</div>
              <div class="rank-badge">总记录 {{ item.totalCount }}</div>
              <div class="rank-badge">异常率 {{ formatPercent(item.abnormalRate) }}</div>
              <div class="rank-badge" :class="riskBadgeClass(item.riskLevel)">风险 {{ item.riskScore ?? 0 }} / {{ riskLevelLabel(item.riskLevel) }}</div>
              <div v-if="item.alertTriggered" class="rank-badge rank-badge-warning">预警</div>
            </div>
          </button>
        </div>
        <div v-if="state.abnormalMonitor.statusCounts.length" class="status-summary-list">
          <div v-for="item in state.abnormalMonitor.statusCounts" :key="`abnormal-${item.checkInStatus}`" class="status-summary-item">
            <span class="attendance-meta">{{ getAttendanceStatusLabel(item.checkInStatus) }}</span>
            <van-tag size="small" :type="getAttendanceStatusTagType(item.checkInStatus)">
              {{ item.count }}
            </van-tag>
          </div>
        </div>
        <div class="reason-distribution-wrap">
          <div class="summary-label">异常原因分布</div>
          <div v-if="state.abnormalMonitor.reasonDistributions.length" class="reason-distribution-list">
            <div v-for="item in state.abnormalMonitor.reasonDistributions" :key="`${item.reasonKey}-${item.reasonTag}`" class="reason-distribution-item">
              <div>
                <div class="rank-title">{{ item.reasonLabel || item.reasonKey || '-' }}</div>
                <div class="attendance-meta">标签：{{ item.reasonTag || '-' }}</div>
              </div>
              <div class="rank-stats">
                <div class="rank-badge">{{ item.count ?? 0 }}次</div>
                <div class="rank-badge">{{ formatPercent(item.rate) }}</div>
              </div>
            </div>
          </div>
          <div v-else class="panel-hint">当前范围暂无异常原因分布数据。</div>
        </div>
      </template>
    </section>
    </details>

    <section class="panel">
      <div class="panel-title">异常用户趋势</div>
      <div class="panel-hint">点击上方异常用户后，按日期查看该用户的异常次数、总记录数和异常率。</div>
      <van-loading v-if="state.trendLoading" class="state-block" size="24px" vertical>趋势加载中...</van-loading>
      <template v-else>
        <van-empty v-if="!state.trendUser" description="请先从异常用户榜单选择一个用户" />
        <div v-else class="trend-wrap">
          <div v-if="state.abnormalUserSummary" class="summary-card user-summary-card">
            <div class="summary-label">用户摘要</div>
            <div class="attendance-meta">姓名：{{ state.abnormalUserSummary.realName || '-' }}</div>
            <div class="attendance-meta">账号：{{ state.abnormalUserSummary.username || '-' }}</div>
            <div class="attendance-meta">部门/单位：{{ state.abnormalUserSummary.unitName || '-' }}</div>
            <div class="attendance-meta">最近异常日期：{{ state.abnormalUserSummary.recentAbnormalDate || '-' }}</div>
            <div class="attendance-meta">最近异常类型：{{ resultLabel(state.abnormalUserSummary.recentAbnormalType) }}</div>
            <div class="attendance-meta">异常总次数：{{ state.abnormalUserSummary.abnormalCount ?? 0 }}</div>
            <div class="attendance-meta">风险等级：{{ state.abnormalUserSummary.riskScore ?? 0 }} / {{ riskLevelLabel(state.abnormalUserSummary.riskLevel) }}</div>
            <div class="attendance-meta">最近7天趋势：{{ trendDirectionLabel(state.abnormalUserSummary.trendDirection) }}（{{ state.abnormalUserSummary.recent7DayAbnormalCount ?? 0 }} vs {{ state.abnormalUserSummary.previous7DayAbnormalCount ?? 0 }}）</div>
            <div class="attendance-meta">主要异常原因：{{ state.abnormalUserSummary.mainReasonLabel || '-' }} / {{ state.abnormalUserSummary.mainReasonTag || '-' }}</div>
            <div class="attendance-meta">地点集中度：{{ state.abnormalUserSummary.topLocation || '-' }}（{{ state.abnormalUserSummary.topLocationCount ?? 0 }}次，{{ formatPercent(state.abnormalUserSummary.locationConcentrationRate) }}）</div>
            <div class="attendance-meta">时间段分析：上午 {{ state.abnormalUserSummary.morningAbnormalCount ?? 0 }} / 下午 {{ state.abnormalUserSummary.afternoonAbnormalCount ?? 0 }} / 晚间 {{ state.abnormalUserSummary.eveningAbnormalCount ?? 0 }}</div>
            <div class="attendance-meta">异常高发时段：{{ state.abnormalUserSummary.peakTimeSlot || '-' }}</div>
            <div class="attendance-meta">预警状态：{{ state.abnormalUserSummary.alertTriggered ? '已触发' : '未触发' }}</div>
            <div v-if="state.abnormalUserSummary.alertTriggered" class="attendance-meta">预警规则：{{ state.abnormalUserSummary.alertRuleText || '已触发高风险预警' }}</div>
          </div>
          <van-empty v-else description="当前用户暂无异常摘要" />
          <div v-if="trendOverview.hasData" class="summary-grid trend-overview-grid">
            <div class="summary-card">
              <div class="summary-label">异常总次数</div>
              <div class="summary-value summary-value-danger">{{ trendOverview.abnormalCount }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">总打卡次数</div>
              <div class="summary-value">{{ trendOverview.totalCount }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">平均异常率</div>
              <div class="summary-value">{{ formatPercent(trendOverview.averageAbnormalRate) }}</div>
            </div>
            <div class="summary-card">
              <div class="summary-label">异常天数</div>
              <div class="summary-value">{{ trendOverview.abnormalDays }}</div>
            </div>
          </div>
          <van-empty v-else description="当前趋势范围暂无总览统计" />
          <div class="trend-header">
            <div class="attendance-meta">当前用户：{{ state.trendUser.realName || state.trendUser.username || `用户${state.trendUser.userId}` }}</div>
            <div class="attendance-meta">趋势范围：{{ trendRangeText }}</div>
            <div class="attendance-meta">趋势点数：{{ state.abnormalTrend.length }}</div>
            <div v-if="state.trendDateLinkage.selectedDate" class="attendance-meta">已联动日期：{{ state.trendDateLinkage.selectedDate }}</div>
          </div>
          <div v-if="trendLinkageHint" class="panel-hint">{{ trendLinkageHint }}</div>
          <div v-if="state.trendDateLinkage.selectedDate" class="panel-actions">
            <van-button size="small" plain type="primary" :disabled="pageBusy" @click="handleClearTrendDateLinkage">清除当天联动</van-button>
          </div>
          <van-empty v-if="!state.abnormalTrend.length" description="当前时间范围内暂无趋势数据" />
          <div v-else class="trend-chart">
            <button
              v-for="item in trendBars"
              :key="item.attendanceDate"
              type="button"
              class="trend-bar-col"
              :class="{ 'trend-bar-col-active': state.trendDateLinkage.selectedDate === item.attendanceDate }"
              @click="handleTrendDateClick(item)"
            >
              <div class="trend-bar-stack">
                <div class="trend-bar trend-bar-total" :style="{ height: `${item.totalHeight}%` }"></div>
                <div class="trend-bar trend-bar-abnormal" :style="{ height: `${item.abnormalHeight}%` }"></div>
              </div>
              <div class="trend-date">{{ item.shortDate }}</div>
              <div class="trend-meta">异常 {{ item.abnormalCount }}/总 {{ item.totalCount }}</div>
              <div class="trend-meta">异常率 {{ formatPercent(item.abnormalRate) }}</div>
            </button>
          </div>
        </div>
      </template>
    </section>

    <section class="panel">
      <div class="panel-title">当前打卡点</div>
      <div v-if="state.locationLoading" class="panel-hint">打卡点信息加载中...</div>
      <div v-else class="location-card">
        <div class="attendance-meta">单位：{{ state.locationInfo.unitName || '-' }}</div>
        <div class="attendance-meta">打卡点：{{ state.locationInfo.locationName || '-' }}</div>
        <div class="attendance-meta">地址：{{ state.locationInfo.address || '-' }}</div>
        <div class="attendance-meta">半径：{{ state.locationInfo.radiusMeters ? `${state.locationInfo.radiusMeters}米` : '-' }}</div>
          <div class="attendance-meta">状态：{{ state.locationInfo.allowCheckIn ? '可打卡' : '不可打卡' }}</div>
          <div class="attendance-meta">说明：{{ state.locationInfo.reason || '当前单位已配置主打卡点' }}</div>
          <div v-if="locationGuide" class="attendance-guide">{{ locationGuide }}</div>
        </div>
    </section>

    <section class="panel">
      <div class="panel-title">本次打卡结果</div>
      <div class="panel-hint">当前链路已接入浏览器定位、后端距离校验与结果回显。</div>
      <div class="location-card">
        <div class="attendance-meta">结果：{{ state.checkInResult.success === null ? '未打卡' : state.checkInResult.success ? '打卡成功' : '打卡失败' }}</div>
        <div class="attendance-meta">动作：{{ state.checkInResult.action || '-' }}</div>
        <div class="attendance-meta">定位阶段：{{ state.checkInVisualization.stageText || '-' }}</div>
        <div class="attendance-meta">可用定位：{{ state.checkInVisualization.stageText ? (state.checkInVisualization.hasUsableLocation ? '已获取' : '未获取') : '-' }}</div>
        <div class="attendance-meta">距离：{{ state.checkInResult.distanceMeters == null ? '-' : `${state.checkInResult.distanceMeters}米` }}</div>
        <div class="attendance-meta">允许打卡：{{ state.checkInResult.allowCheckIn === null ? '-' : state.checkInResult.allowCheckIn ? '是' : '否' }}</div>
        <div class="attendance-meta">原因：{{ state.checkInResult.reason || '-' }}</div>
        <div class="attendance-meta">判定分支：{{ state.checkInVisualization.decisionBranch || '-' }}</div>
        <div class="attendance-meta">弱定位容错：{{ state.checkInVisualization.weakToleranceApplied ? '已命中' : '未命中' }}</div>
      </div>
      <div class="panel-title panel-title-sub">定位可视化地图</div>
      <div class="panel-hint">当前地图仅展示最终提交定位点。红色表示打卡点与允许范围，蓝色表示最终提交定位与定位精度范围，重叠关系仅用于辅助诊断，不直接决定放行。</div>
      <div v-if="state.checkInVisualization.error" class="attendance-guide">{{ state.checkInVisualization.error }}</div>
      <div ref="checkInMapRef" class="attendance-map"></div>
      <div class="diagnostic-grid">
        <div class="attendance-meta">当前定位经纬度：{{ checkInCurrentCoordinateText }}</div>
        <div class="attendance-meta">打卡点经纬度：{{ checkInTargetCoordinateText }}</div>
        <div class="attendance-meta">允许半径：{{ checkInAllowedRadiusText }}</div>
        <div class="attendance-meta">accuracy：{{ checkInAccuracyText }}</div>
        <div class="attendance-meta">当前距离：{{ checkInDistanceText }}</div>
        <div class="attendance-meta">是否重叠：{{ checkInOverlapText }}</div>
        <div class="attendance-meta">当前判定结果：{{ checkInDecisionText }}</div>
        <div v-if="state.checkInResult.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID" class="attendance-guide">当前定位无效，若地图仍能看到位置，仅作诊断参考，请检查权限或定位服务。</div>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">{{ state.form.id ? `编辑/补录考勤 #${state.form.id}` : '补录考勤' }}</div>
      <div class="panel-hint">统一 save 接口支持新增补录和回填修改。补录用户优先支持按姓名或手机号搜索，未选择时仍默认当前登录用户。</div>
      <van-field
        v-model.trim="state.userPicker.keyword"
        label="搜索用户"
        placeholder="请输入姓名或手机号"
        :disabled="pageBusy"
        @keyup.enter="handleSearchFormUsers"
      />
      <div class="panel-actions">
        <van-button
          size="small"
          plain
          type="primary"
          :loading="state.userPicker.loading"
          :disabled="pageBusy || !state.userPicker.keyword.trim()"
          @click="handleSearchFormUsers"
        >
          搜索用户
        </van-button>
        <van-button
          v-if="state.form.selectedUser"
          size="small"
          plain
          :disabled="pageBusy"
          @click="handleClearSelectedFormUser"
        >
          改回当前登录用户
        </van-button>
      </div>
      <div v-if="state.form.selectedUser" class="selected-user-card">
        <div class="summary-label">已选补录用户</div>
        <div class="attendance-meta">姓名：{{ state.form.selectedUser.realName || '-' }}</div>
        <div class="attendance-meta">账号：{{ state.form.selectedUser.username || '-' }}</div>
        <div class="attendance-meta">手机号：{{ state.form.selectedUser.mobile || '-' }}</div>
        <div class="attendance-meta">组织：{{ state.form.selectedUser.unitName || '-' }}</div>
        <div class="attendance-meta">用户ID：{{ state.form.selectedUser.id || '-' }}</div>
      </div>
      <div v-else class="attendance-meta">当前未指定补录用户，保存时将默认使用当前登录用户。</div>
      <div v-if="state.userPicker.candidates.length" class="user-candidate-list">
        <button
          v-for="item in state.userPicker.candidates"
          :key="item.id"
          type="button"
          class="user-candidate-item"
          :class="{ 'user-candidate-item-active': Number(state.form.userId || 0) === item.id }"
          :disabled="pageBusy"
          @click="handleSelectFormUser(item)"
        >
          <div class="user-candidate-title">{{ item.realName || item.username || `用户${item.id}` }}</div>
          <div class="attendance-meta">手机号：{{ item.mobile || '-' }}</div>
          <div class="attendance-meta">账号：{{ item.username || '-' }}</div>
          <div class="attendance-meta">组织：{{ item.unitName || '-' }}</div>
        </button>
      </div>
      <div v-else-if="state.userPicker.searched && !state.userPicker.loading" class="attendance-meta">未找到匹配用户，请尝试姓名或手机号关键字。</div>
      <van-field v-model="state.form.attendanceDate" label="考勤日期" type="date" :disabled="pageBusy" />
      <van-field v-model="state.form.checkInTime" label="上班时间" type="datetime-local" :disabled="pageBusy" />
      <van-field v-model="state.form.checkOutTime" label="下班时间" type="datetime-local" :disabled="pageBusy" />
      <van-field v-model="state.form.checkInAddress" label="上班地点" placeholder="请输入上班地点" :disabled="pageBusy" />
      <van-field v-model="state.form.checkOutAddress" label="下班地点" placeholder="请输入下班地点" :disabled="pageBusy" />
      <div class="select-field">
        <span class="select-label">状态</span>
        <select v-model="state.form.validFlag" :disabled="pageBusy">
          <option :value="1">有效</option>
          <option :value="0">无效</option>
        </select>
      </div>
      <div class="panel-actions">
        <van-button size="small" type="primary" :loading="state.saving" :disabled="pageBusy" @click="handleSave">保存</van-button>
        <van-button size="small" plain :disabled="pageBusy" @click="resetForm">清空</van-button>
      </div>
    </section>

    <section class="panel">
      <div class="panel-title">考勤列表</div>
      <div class="panel-hint">
        共 {{ state.total }} 条考勤记录
        <template v-if="state.queryForm.checkInStatus">
          ，当前筛选：{{ getAttendanceStatusLabel(state.queryForm.checkInStatus) }}
        </template>
      </div>
      <div class="attendance-meta">当前查询范围：{{ listRangeText }}</div>
      <div v-if="listPageSummary" class="attendance-meta">{{ listPageSummary }}</div>
      <div v-if="listFilterSummary" class="attendance-meta">当前筛选摘要：{{ listFilterSummary }}</div>
      <div v-if="listLinkageSummary" class="attendance-meta">当前联动：{{ listLinkageSummary }}</div>

      <van-loading v-if="state.loading" class="state-block" size="24px" vertical>加载中...</van-loading>
      <van-empty v-else-if="!state.list.length" description="暂无考勤数据">
        <template #default>
          <div class="panel-hint">可直接点击上方打卡按钮，或在上方表单补录一条考勤。</div>
          <div class="attendance-meta">当前查询范围：{{ listRangeText }}</div>
          <div v-if="listFilterSummary" class="attendance-meta">当前筛选摘要：{{ listFilterSummary }}</div>
          <div v-if="listLinkageSummary" class="attendance-meta">当前联动：{{ listLinkageSummary }}</div>
          <div v-if="state.abnormalSelection.activeUserId || state.trendDateLinkage.selectedDate" class="panel-actions">
            <van-button size="small" plain type="primary" :disabled="pageBusy" @click="handleClearAbnormalSelection">
              恢复异常联动前筛选
            </van-button>
          </div>
          <div v-if="hasResettableQueryConditions" class="panel-actions">
            <van-button size="small" plain :disabled="pageBusy" @click="handleReset">
              清空当前查询条件
            </van-button>
          </div>
        </template>
      </van-empty>

      <div v-else class="list-wrap">
        <van-card v-for="item in state.list" :key="item.id" class="attendance-card">
          <template #title>
            <div class="attendance-title">
              <span>{{ item.realName || item.username }} · {{ item.attendanceDate || '-' }}</span>
              <div class="attendance-tag-row">
                <van-tag :type="Number(item.validFlag) === 1 ? 'success' : 'danger'">{{ Number(item.validFlag) === 1 ? '有效' : '无效' }}</van-tag>
                <van-tag v-if="isAbnormalStatus(item.checkInResult)" type="danger" plain>异常</van-tag>
              </div>
            </div>
          </template>
          <template #desc>
        <div class="attendance-meta">用户ID：{{ item.userId }}</div>
        <div class="attendance-meta">账号：{{ item.username || '-' }}</div>
        <div class="attendance-meta">组织：{{ item.unitName || '-' }}</div>
        <div class="attendance-meta">
          结果：
          <van-tag size="small" :type="resultTagType(item.checkInResult)">{{ resultLabel(item.checkInResult) }}</van-tag>
        </div>
        <div class="attendance-meta">状态值：{{ item.checkInResult || '-' }}</div>
        <div class="attendance-meta">距离：{{ item.checkInDistanceMeters == null ? '-' : `${item.checkInDistanceMeters}米` }}</div>
        <div class="attendance-meta">失败原因：{{ item.checkInFailReason || '-' }}</div>
        <div class="attendance-meta">经度：{{ formatCoordinate(item.checkInLongitude) }}</div>
        <div class="attendance-meta">纬度：{{ formatCoordinate(item.checkInLatitude) }}</div>
        <div class="attendance-meta">上班时间：{{ formatDateTime(item.checkInTime) }}</div>
        <div class="attendance-meta">下班时间：{{ formatDateTime(item.checkOutTime) }}</div>
            <div class="attendance-meta">上班地点：{{ item.checkInAddress || '-' }}</div>
            <div class="attendance-meta">下班地点：{{ item.checkOutAddress || '-' }}</div>
          </template>
          <template #footer>
            <div class="action-row">
              <van-button size="small" plain type="warning" :disabled="pageBusy" @click="openEditForm(item)">编辑/补录</van-button>
              <van-button
                size="small"
                plain
                type="danger"
                :loading="state.deletingId === item.id"
                :disabled="pageBusy"
                @click="handleDelete(item)"
              >
                删除
              </van-button>
            </div>
          </template>
        </van-card>
        <van-pagination
          v-if="state.total > state.pageSize"
          v-model="state.pageNo"
          :total-items="state.total"
          :items-per-page="state.pageSize"
          mode="simple"
          @change="handlePageChange"
        />
      </div>
    </section>
    </template>
  </AppPageShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AttendanceLeadershipDetailPanel from '@/components/attendance/AttendanceLeadershipDetailPanel.vue'
import AttendanceLeadershipMemberCards from '@/components/attendance/AttendanceLeadershipMemberCards.vue'
import AttendanceLeadershipSummarySection from '@/components/attendance/AttendanceLeadershipSummarySection.vue'
import PersonalAttendanceWorkspace from '@/components/attendance/PersonalAttendanceWorkspace.vue'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageHelp from '@/components/PageHelp.vue'
import { queryUserPageApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { loadAmapSdk } from '@/utils/amap'
import { LOG_TYPES, reportLog } from '@/utils/log-center'
import {
  buildCheckInDiagnosticsFromCoordinates,
  buildLocationEnvironmentDiagnostics,
  getUserLocation
} from '@/utils/location'
import {
  buildDesktopLocationHint,
  buildLocationDiagnosticPayload,
  classifyLocationAccuracy,
  isDesktopLocationEnvironment,
  resolveLocationAccuracyPolicy,
  resolveLocationFailureMessage
} from '@/utils/location-diagnostics'
import { queryWechatJsapiConfigApi } from '@/api/wechat'
import { getWechatLocationByJsapi, isWechatBrowser } from '@/utils/wechat-jsapi'
import {
  ATTENDANCE_CHECK_IN_STATUS,
  ATTENDANCE_STATUS_OPTIONS,
  getAttendanceStatusLabel,
  getAttendanceStatusTagType
} from '@/constants/attendance'
import {
  checkInApi,
  queryAttendanceAbnormalMonitorApi,
  queryAttendanceAbnormalTrendApi,
  queryAttendanceAbnormalUserSummaryApi,
  deleteAttendanceApi,
  queryAttendanceListApi,
  queryAttendanceSummaryApi,
  queryCurrentAttendanceLocationApi,
  saveAttendanceApi
} from '@/api/attendance'

const userStore = useUserStore()
const QUICK_RANGE_OPTIONS = [
  { key: 'today', label: '今天' },
  { key: 'last7Days', label: '近7天' },
  { key: 'thisMonth', label: '本月' }
]
const GEOLOCATION_SAMPLE_COUNT = 3
const GEOLOCATION_TIMEOUT_MS = 8000
const WECHAT_JSAPI_DEFAULT_PRIORITY = 'WECHAT_FIRST'
const WECHAT_JSAPI_DEFAULT_FALLBACK = 'BROWSER'
const WECHAT_JSAPI_DEFAULT_LOCATION_TYPE = 'gcj02'
const CHECK_IN_FAILURE_MESSAGE = '打卡失败，请稍后重试'
const CHECK_IN_SUCCESS_MESSAGE = '上班打卡成功'
const CHECK_OUT_SUCCESS_MESSAGE = '下班打卡成功'
const CHECK_IN_MAP_DEFAULT_ZOOM = 14
const CHECK_IN_MAP_COLORS = {
  target: '#dc2626',
  current: '#2563eb',
  line: '#0f766e'
}
const LEADERSHIP_STATUS_FILTER_OPTIONS = [
  { label: '全部状态', value: 'ALL' },
  { label: '正常', value: 'normal' },
  { label: '迟到', value: 'late' },
  { label: '未打卡', value: 'missing' },
  { label: '外勤', value: 'field' },
  { label: '异常', value: 'abnormal' }
]
const LEADERSHIP_WORKDAY_LABELS = ['周一', '周二', '周三', '周四', '周五']
const LEADERSHIP_STATUS_RULES = Object.freeze({
  successResults: [
    ATTENDANCE_CHECK_IN_STATUS.CHECK_IN_SUCCESS,
    ATTENDANCE_CHECK_IN_STATUS.CHECK_OUT_SUCCESS
  ],
  structuredFieldResults: [ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE],
  lateKeywords: ['迟到', 'late'],
  fieldKeywords: ['外勤', 'field', 'outside']
})

const checkInMapRef = ref(null)

const state = reactive({
  loading: false,
  locationLoading: false,
  summaryLoading: false,
  abnormalLoading: false,
  trendLoading: false,
  userSummaryLoading: false,
  saving: false,
  checkingIn: false,
  exporting: false,
  abnormalExporting: false,
  deletingId: null,
  pageNo: 1,
  pageSize: 10,
  total: 0,
  list: [],
  summary: {
    totalCount: 0,
    successCount: 0,
    abnormalCount: 0,
    statusCounts: []
  },
  abnormalMonitor: {
    topUsers: [],
    statusCounts: [],
    reasonDistributions: [],
    highRiskCount: 0,
    alertCount: 0
  },
  abnormalTrend: [],
  abnormalUserSummary: null,
  trendUser: null,
  trendDateLinkage: {
    selectedDate: '',
    backupDateFrom: '',
    backupDateTo: ''
  },
  abnormalSelection: {
    activeUserId: null,
    backupKeywords: '',
    backupUserId: null,
    backupAbnormalOnly: false,
    backupCheckInStatus: '',
    backupDateFrom: '',
    backupDateTo: ''
  },
  locationInfo: {
    unitName: '',
    locationName: '',
    address: '',
    latitude: null,
    longitude: null,
    radiusMeters: null,
    accuracyGoodThreshold: 100,
    accuracyMaxThreshold: 1000,
    allowCheckIn: false,
    status: '',
    reason: ''
  },
  checkInResult: {
    success: null,
    allowCheckIn: null,
    action: '',
    status: '',
    distanceMeters: null,
    reason: ''
  },
  checkInVisualization: {
    rawLatitude: null,
    rawLongitude: null,
    convertedLatitude: null,
    convertedLongitude: null,
    submitLatitude: null,
    submitLongitude: null,
    targetLatitude: null,
    targetLongitude: null,
    radiusMeters: null,
    accuracyMeters: null,
    localDistanceMeters: null,
    overlap: false,
    decisionBranch: '',
    weakToleranceApplied: false,
    error: '',
    stageText: '',
    hasUsableLocation: false,
    sampleTimestamp: null
  },
  userPicker: {
    loading: false,
    searched: false,
    keyword: '',
    candidates: []
  },
  queryForm: {
    keywords: '',
    unitName: '',
    checkInStatus: '',
    userId: null,
    abnormalOnly: false,
    dateFrom: '',
    dateTo: ''
  },
  leadershipLoading: false,
  leadershipDetailLoading: false,
  leadershipScopeDescription: '',
  leadershipUsers: [],
  leadershipTodayRecords: [],
  leadershipWeekRecords: [],
  leadershipRecentAbnormalRecords: [],
  leadershipSelectedUserId: null,
  leadershipFilters: {
    keyword: '',
    status: 'ALL',
    department: 'ALL'
  },
  form: createEmptyForm()
})

let checkInMapInstance = null
let checkInAmap = null
let checkInMapElements = null

const pageBusy = computed(() => {
  return state.loading || state.summaryLoading || state.abnormalLoading || state.trendLoading || state.userSummaryLoading || state.saving || state.checkingIn || state.exporting || state.abnormalExporting || state.deletingId !== null
})

const abnormalRate = computed(() => {
  const total = Number(state.summary.totalCount || 0)
  const abnormal = Number(state.summary.abnormalCount || 0)
  if (!total) {
    return '0%'
  }
  return `${((abnormal / total) * 100).toFixed(1)}%`
})

const trendBars = computed(() => {
  const maxTotal = Math.max(...state.abnormalTrend.map((item) => Number(item.totalCount || 0)), 0)
  return state.abnormalTrend.map((item) => {
    const total = Number(item.totalCount || 0)
    const abnormal = Number(item.abnormalCount || 0)
    return {
      ...item,
      shortDate: String(item.attendanceDate || '').slice(5),
      totalHeight: maxTotal ? Math.max((total / maxTotal) * 100, total > 0 ? 8 : 0) : 0,
      abnormalHeight: maxTotal ? Math.max((abnormal / maxTotal) * 100, abnormal > 0 ? 8 : 0) : 0
    }
  })
})

const trendOverview = computed(() => {
  const points = Array.isArray(state.abnormalTrend) ? state.abnormalTrend : []
  const totalCount = points.reduce((sum, item) => sum + Number(item.totalCount || 0), 0)
  const abnormalCount = points.reduce((sum, item) => sum + Number(item.abnormalCount || 0), 0)
  const abnormalDays = points.filter((item) => Number(item.abnormalCount || 0) > 0).length
  return {
    hasData: points.length > 0,
    totalCount,
    abnormalCount,
    abnormalDays,
    averageAbnormalRate: totalCount ? (abnormalCount / totalCount) * 100 : 0
  }
})

const leadershipTodayText = computed(() => getTodayDateText())

const leadershipWorkweekDateTexts = computed(() => buildWorkweekDateTexts(leadershipTodayText.value))

const leadershipEffectiveWorkweekDateTexts = computed(() => {
  return leadershipWorkweekDateTexts.value.filter(dateText => dateText <= leadershipTodayText.value)
})

const leadershipWorkweekRangeText = computed(() => {
  const dates = leadershipWorkweekDateTexts.value
  if (!dates.length) {
    return ''
  }
  return `${dates[0]} 至 ${dates[dates.length - 1]}`
})

const leadershipTodayIsNonWorkday = computed(() => {
  return !leadershipWorkweekDateTexts.value.includes(leadershipTodayText.value)
})

const currentUserId = computed(() => Number(userStore.userInfo?.userId || 0))

const leadershipAllEnabledUsers = computed(() => {
  return Array.isArray(state.leadershipUsers) ? state.leadershipUsers.filter(isLeadershipUserEnabled) : []
})

const leadershipSubordinates = computed(() => {
  return leadershipAllEnabledUsers.value.filter(item => Number(item.id || 0) !== currentUserId.value)
})

const hasSubordinates = computed(() => leadershipSubordinates.value.length > 0)

const leadershipEnabledUsers = computed(() => {
  return hasSubordinates.value ? leadershipSubordinates.value : leadershipAllEnabledUsers.value
})

const leadershipDepartmentOptions = computed(() => {
  return Array.from(
    new Set(
      leadershipEnabledUsers.value
        .map(item => item.unitName)
        .filter(Boolean)
    )
  ).sort((left, right) => String(left).localeCompare(String(right), 'zh-Hans-CN'))
})

const leadershipTodayRecordMap = computed(() => buildLatestUserRecordMap(state.leadershipTodayRecords))

const leadershipWeekRecordMap = computed(() => buildGroupedUserRecords(state.leadershipWeekRecords))

function classifyLeadershipTodayWorkspaceStatus(record) {
  if (!leadershipTodayIsNonWorkday.value) {
    return classifyLeadershipStatus(record)
  }
  if (!record) {
    return {
      key: 'rest',
      label: '非工作日',
      tone: 'pending',
      summary: '今日为非工作日，暂无加班 / 值班打卡记录'
    }
  }
  if (!isAbnormalStatus(record.checkInResult)) {
    return {
      key: 'normal',
      label: '加班打卡',
      tone: 'normal',
      summary: '今日为非工作日，已记录加班 / 值班打卡'
    }
  }
  return {
    key: 'abnormal',
    label: '异常',
    tone: 'abnormal',
    summary: record.checkInFailReason || resultLabel(record.checkInResult)
  }
}

const leadershipMemberCards = computed(() => {
  return leadershipEnabledUsers.value.map((user) => {
    const todayRecord = leadershipTodayRecordMap.value.get(user.id) || null
    const weekRecords = leadershipWeekRecordMap.value.get(user.id) || []
    const status = classifyLeadershipTodayWorkspaceStatus(todayRecord)
    const weekSummary = createLeadershipWeekSummary(weekRecords, leadershipWorkweekDateTexts.value)
    return {
      userId: user.id,
      username: user.username || '',
      realName: user.realName || user.username || `用户${user.id}`,
      unitName: user.unitName || '',
      jobTitle: user.jobTitle || '',
      todayRecord,
      weekRecords,
      statusKey: status.key,
      statusLabel: status.label,
      statusTone: status.tone,
      statusSummary: status.summary,
      checkInTimeText: formatTimeOnly(todayRecord?.checkInTime),
      checkOutTimeText: formatTimeOnly(todayRecord?.checkOutTime),
      weekSummary
    }
  })
})

const leadershipFilteredMembers = computed(() => {
  const keyword = String(state.leadershipFilters.keyword || '').trim().toLowerCase()
  const status = state.leadershipFilters.status || 'ALL'
  const department = state.leadershipFilters.department || 'ALL'
  return leadershipMemberCards.value.filter((member) => {
    const matchesKeyword = !keyword || [
      member.realName,
      member.username,
      member.jobTitle,
      member.unitName
    ]
      .filter(Boolean)
      .some(item => String(item).toLowerCase().includes(keyword))
    const matchesStatus = status === 'ALL' || member.statusKey === status
    const matchesDepartment = department === 'ALL' || member.unitName === department
    return matchesKeyword && matchesStatus && matchesDepartment
  })
})

const selectedLeadershipMember = computed(() => {
  return leadershipFilteredMembers.value.find(item => item.userId === state.leadershipSelectedUserId) || null
})

function summarizeMemberNames(items, emptyText) {
  if (!items.length) {
    return emptyText
  }
  const names = items.slice(0, 6).map(item => item.realName || item.username || `用户${item.userId}`)
  const suffix = items.length > 6 ? ` 等 ${items.length} 人` : ` 共 ${items.length} 人`
  return `${names.join('、')}${suffix}`
}

const leadershipOverview = computed(() => {
  const members = leadershipFilteredMembers.value
  const isNonWorkday = leadershipTodayIsNonWorkday.value
  const expectedCount = isNonWorkday ? 0 : members.length
  const checkedCount = isNonWorkday ? 0 : members.filter(item => item.todayRecord).length
  const missingCount = isNonWorkday ? 0 : members.filter(item => item.statusKey === 'missing').length
  const lateCount = isNonWorkday ? 0 : members.filter(item => item.statusKey === 'late').length
  const fieldCount = isNonWorkday ? 0 : members.filter(item => item.statusKey === 'field').length
  const totalAttendanceDays = members.reduce((sum, item) => sum + Number(item.weekSummary.attendanceDays || 0), 0)
  const attendanceBase = members.length * leadershipEffectiveWorkweekDateTexts.value.length
  const weeklyAttendanceRate = attendanceBase ? (totalAttendanceDays / attendanceBase) * 100 : 0
  return {
    isNonWorkday,
    expectedCount,
    checkedCount,
    missingCount,
    lateCount,
    fieldCount,
    weeklyAttendanceRate,
    weeklyAttendanceRateText: formatPercent(weeklyAttendanceRate)
  }
})

const leadershipAlerts = computed(() => {
  if (leadershipTodayIsNonWorkday.value) {
    return {
      isNonWorkday: true,
      missingCount: 0,
      lateCount: 0,
      abnormalCount: 0,
      missingSummary: '今日为非工作日，不生成未打卡提醒',
      lateSummary: '今日为非工作日，不生成迟到提醒',
      abnormalSummary: '非工作日打卡请查看上方加班/值班提示，不并入工作日异常统计'
    }
  }
  const members = leadershipFilteredMembers.value
  const missingMembers = members.filter(item => item.statusKey === 'missing')
  const lateMembers = members.filter(item => item.statusKey === 'late')
  const abnormalMembers = members.filter(item => item.statusKey === 'abnormal' || item.statusKey === 'field')
  return {
    isNonWorkday: false,
    missingCount: missingMembers.length,
    lateCount: lateMembers.length,
    abnormalCount: abnormalMembers.length,
    missingSummary: summarizeMemberNames(missingMembers, '当前没有未打卡成员'),
    lateSummary: summarizeMemberNames(lateMembers, '当前没有迟到成员'),
    abnormalSummary: summarizeMemberNames(abnormalMembers, '当前没有定位或外勤异常成员')
  }
})

const leadershipNonWorkdayNotice = computed(() => {
  if (!leadershipTodayIsNonWorkday.value) {
    return null
  }
  const overtimeMembers = leadershipFilteredMembers.value.filter(item => item.todayRecord)
  return {
    title: '今日为非工作日，打卡按加班 / 值班记录处理',
    description: '今日打卡不会并入工作日应到、未打卡或迟到异常统计，请单独查看非工作日打卡情况。',
    countLabel: '加班打卡人数',
    count: overtimeMembers.length,
    summary: overtimeMembers.length
      ? `已记录加班 / 值班打卡：${summarizeMemberNames(overtimeMembers, '')}`
      : '当前范围内还没有加班 / 值班打卡记录。'
  }
})

const selectedLeadershipDetail = computed(() => {
  if (!selectedLeadershipMember.value) {
    return {
      todayRecord: null,
      weekSummary: createLeadershipWeekSummary([], leadershipWorkweekDateTexts.value),
      trendItems: buildLeadershipTrendItems([], leadershipWorkweekDateTexts.value),
      recentAbnormalRecords: []
    }
  }
  return {
    todayRecord: buildLeadershipTodayDetailRecord(selectedLeadershipMember.value.todayRecord),
    weekSummary: selectedLeadershipMember.value.weekSummary,
    trendItems: buildLeadershipTrendItems(selectedLeadershipMember.value.weekRecords, leadershipWorkweekDateTexts.value),
    recentAbnormalRecords: state.leadershipRecentAbnormalRecords.map(buildLeadershipAbnormalRecordItem)
  }
})

const currentWorkspaceUser = computed(() => {
  return leadershipAllEnabledUsers.value.find(item => Number(item.id || 0) === currentUserId.value) || null
})

const currentWorkspaceUserName = computed(() => {
  return currentWorkspaceUser.value?.realName || userStore.userInfo?.realName || userStore.userInfo?.username || '当前用户'
})

const personalWorkspaceLoading = computed(() => state.locationLoading || state.loading || state.leadershipLoading)
const locationAccuracyPolicy = computed(() => resolveLocationAccuracyPolicy({
  goodThreshold: state.locationInfo.accuracyGoodThreshold,
  maxThreshold: state.locationInfo.accuracyMaxThreshold
}))

const personalTodayRecord = computed(() => {
  return leadershipTodayRecordMap.value.get(currentUserId.value) || null
})

const personalWeekRecords = computed(() => {
  return leadershipWeekRecordMap.value.get(currentUserId.value) || []
})

const personalWorkspaceStatus = computed(() => {
  const status = classifyLeadershipTodayWorkspaceStatus(personalTodayRecord.value)
  return {
    label: status.label,
    tone: status.tone
  }
})

const personalWorkspaceNotice = computed(() => {
  if (isDesktopLocationEnvironment()) {
    return {
      title: '设备定位提示',
      text: buildDesktopLocationHint()
    }
  }
  if (!leadershipTodayIsNonWorkday.value) {
    return null
  }
  return personalTodayRecord.value
    ? {
        title: '今日为非工作日',
        text: '已记录加班 / 值班打卡，当前记录已生效，但不会计入工作日应到或未打卡异常。'
      }
    : {
        title: '今日为非工作日',
        text: '今天可按加班 / 值班记录打卡；未打卡不会计入工作日未打卡异常。'
      }
})

const personalWorkspaceTodayCard = computed(() => {
  const todayRecord = personalTodayRecord.value
  const locationText = todayRecord?.checkInAddress
    || todayRecord?.checkOutAddress
    || state.locationInfo.locationName
    || state.locationInfo.address
    || '未获取到定位信息'
  const locationDetail = todayRecord
    ? `${leadershipTodayIsNonWorkday.value ? '今日已记录加班 / 值班打卡' : '今日记录'}：${resultLabel(todayRecord.checkInResult)}`
    : (leadershipTodayIsNonWorkday.value
        ? '今日为非工作日，暂未产生加班 / 值班打卡记录。'
        : (state.locationInfo.address || state.locationInfo.reason || '今日尚未产生打卡记录。'))
  const locationStatus = state.checkInResult.reason
    || state.locationInfo.reason
    || ''

  return {
    checkInTime: formatTimeOnly(todayRecord?.checkInTime),
    checkOutTime: formatTimeOnly(todayRecord?.checkOutTime),
    locationText,
    locationDetail,
    locationStatus
  }
})

const personalWorkspaceWeekStats = computed(() => {
  return createLeadershipWeekSummary(personalWeekRecords.value, leadershipWorkweekDateTexts.value)
})

const personalRecentRecords = computed(() => {
  return state.list.slice(0, 5).map((item, index) => ({
    id: item.id,
    recordKey: `${item.userId || 'self'}-${item.attendanceDate || index}`,
    dateText: item.attendanceDate || '-',
    statusLabel: resultLabel(item.checkInResult),
    timeText: `${formatTimeOnly(item.checkInTime)} / ${formatTimeOnly(item.checkOutTime)}`,
    addressText: item.checkInAddress || item.checkOutAddress || item.checkInFailReason || '暂无地点信息'
  }))
})

function hasCompletedCheckOut(record) {
  return Boolean(record?.checkOutTime)
    || record?.checkInResult === ATTENDANCE_CHECK_IN_STATUS.CHECK_OUT_SUCCESS
    || record?.checkInResult === ATTENDANCE_CHECK_IN_STATUS.ALREADY_FINISHED
}

function hasCompletedCheckIn(record) {
  return hasCompletedCheckOut(record)
    || Boolean(record?.checkInTime)
    || record?.checkInResult === ATTENDANCE_CHECK_IN_STATUS.CHECK_IN_SUCCESS
}

const personalCheckInButtonText = computed(() => {
  if (hasCompletedCheckOut(personalTodayRecord.value)) {
    return '今日已完成'
  }
  if (hasCompletedCheckIn(personalTodayRecord.value)) {
    return '下班打卡'
  }
  return '上班打卡'
})

const personalCanCheckIn = computed(() => {
  return !hasCompletedCheckOut(personalTodayRecord.value)
    && !state.locationLoading
    && Boolean(state.locationInfo.allowCheckIn)
})

const personalWorkspaceCheckInHint = computed(() => {
  if (hasCompletedCheckOut(personalTodayRecord.value)) {
    return '今日上下班打卡已完成，无需重复提交。'
  }
  if (!personalCanCheckIn.value) {
    return state.locationInfo.reason || '当前打卡点不可用，请联系管理员检查配置。'
  }
  if (state.checkingIn) {
    return '正在获取定位并提交本次打卡...'
  }
  if (hasCompletedCheckIn(personalTodayRecord.value)) {
    return '已完成上班打卡，本次提交将记录下班时间。'
  }
  if (leadershipTodayIsNonWorkday.value) {
    return '今日按加班 / 值班记录处理，打卡成功后会在当前页面单独展示，不并入工作日未打卡统计。'
  }
  return '点击后会自动获取定位、提交打卡并刷新今日状态。'
})

function createEmptyForm() {
  return {
    id: null,
    userId: '',
    selectedUser: null,
    attendanceDate: toInputDate(new Date()),
    checkInTime: '',
    checkOutTime: '',
    checkInAddress: '',
    checkOutAddress: '',
    validFlag: 1
  }
}

function toInputDate(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

function addDays(value, offset) {
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return new Date(NaN)
  }
  date.setDate(date.getDate() + offset)
  return date
}

function startOfMonth(value) {
  const date = value instanceof Date ? new Date(value.getTime()) : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return new Date(NaN)
  }
  date.setDate(1)
  return date
}

function getQuickRangeDates(key) {
  const today = new Date()
  if (key === 'today') {
    const dateText = toInputDate(today)
    return { dateFrom: dateText, dateTo: dateText }
  }
  if (key === 'last7Days') {
    return {
      dateFrom: toInputDate(addDays(today, -6)),
      dateTo: toInputDate(today)
    }
  }
  if (key === 'thisMonth') {
    return {
      dateFrom: toInputDate(startOfMonth(today)),
      dateTo: toInputDate(today)
    }
  }
  return { dateFrom: '', dateTo: '' }
}

function toInputDateTime(value) {
  const date = value instanceof Date ? value : new Date(value)
  if (Number.isNaN(date.getTime())) {
    return ''
  }
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day}T${hours}:${minutes}`
}

function toBackendDateTime(value) {
  if (!value) {
    return ''
  }
  return `${value.replace('T', ' ')}:00`
}

function formatDateTime(value) {
  if (!value) {
    return '-'
  }
  return String(value).replace('T', ' ')
}

function formatCoordinate(value) {
  if (value == null || value === '') {
    return '-'
  }
  return Number(value).toFixed(6)
}

function formatPercent(value) {
  if (value == null || value === '') {
    return '0.0%'
  }
  return `${Number(value).toFixed(1)}%`
}

function formatTimeOnly(value) {
  if (!value) {
    return '-'
  }
  return String(value).slice(11, 16) || '-'
}

function getTodayDateText() {
  return toInputDate(new Date())
}

function getWeekStartDateText(baseDateText = getTodayDateText()) {
  const baseDate = new Date(`${baseDateText}T00:00:00`)
  if (Number.isNaN(baseDate.getTime())) {
    return baseDateText
  }
  const weekDay = baseDate.getDay() || 7
  return toInputDate(addDays(baseDate, 1 - weekDay))
}

function buildWorkweekDateTexts(baseDateText = getTodayDateText()) {
  const mondayText = getWeekStartDateText(baseDateText)
  const mondayDate = new Date(`${mondayText}T00:00:00`)
  return LEADERSHIP_WORKDAY_LABELS.map((_, index) => toInputDate(addDays(mondayDate, index)))
}

function isLeadershipUserEnabled(user) {
  if (user?.status == null) {
    return true
  }
  return Number(user.status) === 1
}

function buildRecordSignalText(record) {
  return [
    record?.checkInResult,
    record?.checkInFailReason,
    record?.checkInAddress,
    record?.checkOutAddress
  ]
    .filter(Boolean)
    .join(' ')
    .toLowerCase()
}

function includesAnySignal(text, keywords = []) {
  return keywords.some(keyword => text.includes(String(keyword).toLowerCase()))
}

function resolveLeadershipStatusSignals(record) {
  const signalText = buildRecordSignalText(record)
  const status = record?.checkInResult || ''
  return {
    status,
    structuredSuccess: LEADERSHIP_STATUS_RULES.successResults.includes(status),
    structuredField: LEADERSHIP_STATUS_RULES.structuredFieldResults.includes(status),
    inferredLate: includesAnySignal(signalText, LEADERSHIP_STATUS_RULES.lateKeywords),
    inferredField: includesAnySignal(signalText, LEADERSHIP_STATUS_RULES.fieldKeywords)
  }
}

function hasLateSignal(record) {
  const signalText = buildRecordSignalText(record)
  return signalText.includes('迟到') || signalText.includes('late')
}

function hasFieldSignal(record) {
  const signalText = buildRecordSignalText(record)
  return record?.checkInResult === ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE
    || signalText.includes('外勤')
    || signalText.includes('field')
    || signalText.includes('outside')
}

function classifyLeadershipStatus(record) {
  if (!record) {
    return {
      key: 'missing',
      label: '未打卡',
      tone: 'missing',
      summary: '今天还没有打卡记录'
    }
  }
  const signals = resolveLeadershipStatusSignals(record)
  if (signals.inferredLate) {
    return {
      key: 'late',
      label: '迟到',
      tone: 'late',
      summary: record.checkInFailReason || '今日记录识别为迟到'
    }
  }
  if (signals.structuredField || signals.inferredField) {
    return {
      key: 'field',
      label: '外勤',
      tone: 'field',
      summary: record.checkInFailReason || '今日记录包含外勤或超范围信号'
    }
  }
  if (signals.structuredSuccess && !isAbnormalStatus(record.checkInResult)) {
    return {
      key: 'normal',
      label: '正常',
      tone: 'normal',
      summary: '今日打卡状态正常'
    }
  }
  return {
    key: 'abnormal',
    label: '异常',
    tone: 'abnormal',
    summary: record.checkInFailReason || resultLabel(record.checkInResult)
  }
}

function getRecordSortValue(record) {
  return String(record?.checkOutTime || record?.checkInTime || record?.createTime || record?.attendanceDate || '')
}

function buildLatestUserRecordMap(records) {
  const result = new Map()
  records.forEach((record) => {
    const userId = Number(record?.userId || 0)
    if (!userId) {
      return
    }
    const current = result.get(userId)
    if (!current || getRecordSortValue(record) > getRecordSortValue(current)) {
      result.set(userId, record)
    }
  })
  return result
}

function buildGroupedUserRecords(records) {
  const result = new Map()
  records.forEach((record) => {
    const userId = Number(record?.userId || 0)
    if (!userId) {
      return
    }
    if (!result.has(userId)) {
      result.set(userId, [])
    }
    result.get(userId).push(record)
  })
  result.forEach((items) => {
    items.sort((left, right) => getRecordSortValue(right).localeCompare(getRecordSortValue(left)))
  })
  return result
}

function buildDailyRecordMap(records) {
  const result = new Map()
  records.forEach((record) => {
    const dateKey = String(record?.attendanceDate || '')
    if (!dateKey) {
      return
    }
    const current = result.get(dateKey)
    if (!current || getRecordSortValue(record) > getRecordSortValue(current)) {
      result.set(dateKey, record)
    }
  })
  return result
}

function createLeadershipWeekSummary(records, workdayDateTexts) {
  const dailyRecordMap = buildDailyRecordMap(records)
  const effectiveDates = workdayDateTexts.filter(dateText => dateText <= getTodayDateText())
  let lateCount = 0
  let fieldCount = 0
  let abnormalCount = 0
  effectiveDates.forEach((dateText) => {
    const status = classifyLeadershipStatus(dailyRecordMap.get(dateText))
    if (status.key === 'late') {
      lateCount += 1
    } else if (status.key === 'field') {
      fieldCount += 1
    } else if (status.key === 'abnormal') {
      abnormalCount += 1
    }
  })
  const attendanceDays = effectiveDates.filter(dateText => dailyRecordMap.has(dateText)).length
  return {
    attendanceDays,
    lateCount,
    fieldCount,
    abnormalCount,
    missingCount: Math.max(effectiveDates.length - attendanceDays, 0)
  }
}

function extractTimeDeviationText(record) {
  const reasonText = record?.checkInFailReason || ''
  if (!reasonText) {
    return ''
  }
  const durationMatch = reasonText.match(/(\d+\s*(分钟|小时))/)
  return durationMatch ? durationMatch[1] : reasonText
}

function buildLeadershipTodayDetailRecord(record) {
  if (!record) {
    return null
  }
  const status = classifyLeadershipStatus(record)
  return {
    checkInTimeText: formatTimeOnly(record.checkInTime),
    checkOutTimeText: formatTimeOnly(record.checkOutTime),
    resultLabel: resultLabel(record.checkInResult),
    rangeLabel: record.checkInResult === ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE ? '否' : '是',
    checkInAddress: record.checkInAddress || '',
    checkOutAddress: record.checkOutAddress || '',
    timeDeviationText: extractTimeDeviationText(record) || (status.key === 'late' ? '已识别迟到，但未返回具体时长' : '')
  }
}

function buildLeadershipTrendItems(records, workdayDateTexts) {
  const dailyRecordMap = buildDailyRecordMap(records)
  const todayText = getTodayDateText()
  return workdayDateTexts.map((dateText, index) => {
    if (dateText > todayText) {
      return {
        date: dateText,
        shortDate: dateText.slice(5),
        weekday: LEADERSHIP_WORKDAY_LABELS[index],
        label: '待发生',
        tone: 'pending'
      }
    }
    const status = classifyLeadershipStatus(dailyRecordMap.get(dateText))
    return {
      date: dateText,
      shortDate: dateText.slice(5),
      weekday: LEADERSHIP_WORKDAY_LABELS[index],
      label: status.label,
      tone: status.tone
    }
  })
}

function buildLeadershipAbnormalRecordItem(record) {
  return {
    id: record?.id,
    attendanceDate: record?.attendanceDate || '-',
    resultLabel: resultLabel(record?.checkInResult),
    checkInAddress: record?.checkInAddress || '',
    checkOutAddress: record?.checkOutAddress || '',
    reasonText: record?.checkInFailReason || resultLabel(record?.checkInResult)
  }
}

async function queryAllUsersByScope(params = {}) {
  const pageSize = 200
  let pageNo = 1
  let total = 0
  let scopeDescription = ''
  const list = []
  do {
    const response = await queryUserPageApi({
      ...params,
      pageNo,
      pageSize
    })
    const data = response?.data || {}
    const pageList = Array.isArray(data.list) ? data.list : []
    total = Number(data.total || pageList.length)
    scopeDescription = data.scopeDescription || scopeDescription
    list.push(...pageList)
    if (!pageList.length || list.length >= total) {
      break
    }
    pageNo += 1
  } while (pageNo <= 20)
  return {
    list,
    scopeDescription
  }
}

async function queryAllAttendanceRecords(payload = {}) {
  const pageSize = 200
  let pageNo = 1
  let total = 0
  const list = []
  do {
    const response = await queryAttendanceListApi({
      ...payload,
      pageNo,
      pageSize
    })
    const data = response?.data || {}
    const pageList = Array.isArray(data.list) ? data.list : []
    total = Number(data.total || pageList.length)
    list.push(...pageList)
    if (!pageList.length || list.length >= total) {
      break
    }
    pageNo += 1
  } while (pageNo <= 20)
  return list
}

async function fetchLeadershipRecentAbnormalRecords(userId) {
  const normalizedUserId = Number(userId || 0)
  if (!normalizedUserId) {
    state.leadershipRecentAbnormalRecords = []
    state.leadershipDetailLoading = false
    return
  }
  state.leadershipDetailLoading = true
  try {
    const response = await queryAttendanceListApi({
      userId: normalizedUserId,
      abnormalOnly: true,
      pageNo: 1,
      pageSize: 3
    })
    if (state.leadershipSelectedUserId !== normalizedUserId) {
      return
    }
    const data = response?.data || {}
    state.leadershipRecentAbnormalRecords = Array.isArray(data.list) ? data.list : []
  } catch (error) {
    if (state.leadershipSelectedUserId === normalizedUserId) {
      state.leadershipRecentAbnormalRecords = []
    }
  } finally {
    if (state.leadershipSelectedUserId === normalizedUserId || !state.leadershipSelectedUserId) {
      state.leadershipDetailLoading = false
    }
  }
}

async function handleSelectLeadershipMember(member) {
  const normalizedUserId = Number(member?.userId || 0)
  state.leadershipSelectedUserId = normalizedUserId || null
  if (!normalizedUserId) {
    state.leadershipRecentAbnormalRecords = []
    state.leadershipDetailLoading = false
    return
  }
  await fetchLeadershipRecentAbnormalRecords(normalizedUserId)
}

function handleLeadershipKeywordChange(value) {
  state.leadershipFilters.keyword = String(value || '')
}

function handleLeadershipStatusChange(value) {
  state.leadershipFilters.status = value || 'ALL'
}

function handleLeadershipDepartmentChange(value) {
  state.leadershipFilters.department = value || 'ALL'
}

function handleResetLeadershipFilters() {
  state.leadershipFilters.keyword = ''
  state.leadershipFilters.status = 'ALL'
  state.leadershipFilters.department = 'ALL'
}

async function fetchLeadershipWorkspace() {
  state.leadershipLoading = true
  try {
    const todayDateText = leadershipTodayText.value
    const workdayDateTexts = leadershipEffectiveWorkweekDateTexts.value
    const weekStartDateText = workdayDateTexts[0] || todayDateText
    const [userScope, todayRecords, weekRecords] = await Promise.all([
      queryAllUsersByScope(),
      queryAllAttendanceRecords({
        dateFrom: todayDateText,
        dateTo: todayDateText
      }),
      queryAllAttendanceRecords({
        dateFrom: weekStartDateText,
        dateTo: todayDateText
      })
    ])
    state.leadershipUsers = Array.isArray(userScope.list) ? userScope.list : []
    state.leadershipScopeDescription = userScope.scopeDescription || ''
    state.leadershipTodayRecords = Array.isArray(todayRecords) ? todayRecords : []
    state.leadershipWeekRecords = Array.isArray(weekRecords) ? weekRecords : []
    if (!hasSubordinates.value) {
      state.leadershipSelectedUserId = null
      state.leadershipRecentAbnormalRecords = []
      state.leadershipDetailLoading = false
      return
    }
    const preferredMember = leadershipFilteredMembers.value.find(item => item.userId === state.leadershipSelectedUserId)
      || leadershipFilteredMembers.value[0]
      || null
    if (!preferredMember) {
      state.leadershipSelectedUserId = null
      state.leadershipRecentAbnormalRecords = []
      state.leadershipDetailLoading = false
      return
    }
    await handleSelectLeadershipMember(preferredMember)
  } catch (error) {
    state.leadershipScopeDescription = ''
    state.leadershipUsers = []
    state.leadershipTodayRecords = []
    state.leadershipWeekRecords = []
    state.leadershipRecentAbnormalRecords = []
    state.leadershipSelectedUserId = null
    state.leadershipDetailLoading = false
    showToast(error.message || '团队考勤工作台加载失败')
  } finally {
    state.leadershipLoading = false
  }
}

function buildOutOfRangeReason(distanceMeters, radiusMeters, accuracyMeters) {
  if (distanceMeters == null || radiusMeters == null) {
    return '当前位置超出打卡范围，请到指定地点后重试'
  }
  return `明确超出打卡范围：当前位置距离打卡点约 ${distanceMeters} 米，允许半径 ${radiusMeters} 米`
}

function buildInvalidLocationReason(accuracyMeters) {
  return resolveLocationFailureMessage({
    errorCode: accuracyMeters != null && Number(accuracyMeters) > locationAccuracyPolicy.value.maxThreshold
      ? 'LOCATION_ACCURACY_INVALID'
      : 'LOCATION_INVALID',
    accuracyMeters,
    policy: locationAccuracyPolicy.value
  })
}

function buildWeakLocationReason(accuracyMeters) {
  return resolveLocationFailureMessage({
    errorCode: 'LOCATION_ACCURACY_WEAK',
    accuracyMeters,
    policy: locationAccuracyPolicy.value
  })
}

function resetCheckInVisualizationSubmission() {
  updateCheckInVisualization({
    rawLatitude: null,
    rawLongitude: null,
    convertedLatitude: null,
    convertedLongitude: null,
    submitLatitude: null,
    submitLongitude: null,
    accuracyMeters: null,
    localDistanceMeters: null,
    overlap: false,
    decisionBranch: '',
    weakToleranceApplied: false,
    error: '',
    stageText: '',
    hasUsableLocation: false,
    sampleTimestamp: null
  })
}

function resolveCheckInPageUrl() {
  if (typeof window === 'undefined' || !window.location) {
    return ''
  }
  return window.location.href.split('#')[0] || ''
}

function normalizeWechatJsapiConfig(data = {}) {
  return {
    enabled: Boolean(data.enabled),
    locationEnabled: Boolean(data.locationEnabled),
    appId: data.appId || '',
    timestamp: data.timestamp ?? null,
    nonceStr: data.nonceStr || '',
    signature: data.signature || '',
    jsApiList: Array.isArray(data.jsApiList) ? data.jsApiList : [],
    locationType: data.locationType || WECHAT_JSAPI_DEFAULT_LOCATION_TYPE,
    debug: Boolean(data.debug),
    priority: data.priority || WECHAT_JSAPI_DEFAULT_PRIORITY,
    fallback: data.fallback || WECHAT_JSAPI_DEFAULT_FALLBACK,
    accuracyThreshold: data.accuracyThreshold == null ? null : Number(data.accuracyThreshold),
    timeoutMs: data.timeoutMs == null ? GEOLOCATION_TIMEOUT_MS : Number(data.timeoutMs),
    loadFailed: Boolean(data.loadFailed),
    loadErrorCode: data.loadErrorCode || '',
    loadErrorMessage: data.loadErrorMessage || '',
    responseStatus: data.responseStatus ?? null
  }
}

async function queryWechatJsapiConfigForCheckIn() {
  const pageUrl = resolveCheckInPageUrl()
  if (!pageUrl) {
    return normalizeWechatJsapiConfig()
  }
  try {
    const response = await queryWechatJsapiConfigApi({ url: pageUrl })
    if (!response || response.code !== 0) {
      const failedConfig = normalizeWechatJsapiConfig({
        loadFailed: true,
        loadErrorCode: 'WECHAT_JSAPI_CONFIG_RESPONSE_FAIL',
        loadErrorMessage: response?.message || '微信 jsapi-config 获取失败'
      })
      await reportLog(LOG_TYPES.WECHAT_JSAPI_ERROR, {
        traceId: response?.traceId || '',
        module: 'WECHAT',
        subModule: 'JSAPI_CONFIG',
        title: '微信 JS-SDK 配置获取失败',
        summary: failedConfig.loadErrorMessage,
        diagnosis: '微信环境下 jsapi-config 返回失败，前端已记录并可按需要切换浏览器定位兜底。',
        errorCode: failedConfig.loadErrorCode,
        message: failedConfig.loadErrorMessage,
        requestUrl: '/wechat/jsapi-config',
        requestMethod: 'GET',
        requestParams: { url: pageUrl },
        rawData: response || null
      })
      return failedConfig
    }
    return normalizeWechatJsapiConfig(response.data || {})
  } catch (error) {
    const failedConfig = normalizeWechatJsapiConfig({
      loadFailed: true,
      loadErrorCode: error?.response?.status === 404 ? 'WECHAT_JSAPI_CONFIG_404' : 'WECHAT_JSAPI_CONFIG_FETCH_FAIL',
      loadErrorMessage: error?.response?.data?.message || error.message || '微信 jsapi-config 获取失败',
      responseStatus: error?.response?.status ?? null
    })
    await reportLog(LOG_TYPES.WECHAT_JSAPI_ERROR, {
      traceId: error?.response?.headers?.['x-trace-id'] || '',
      module: 'WECHAT',
      subModule: 'JSAPI_CONFIG',
      title: '微信 JS-SDK 配置获取失败',
      summary: failedConfig.loadErrorMessage,
      diagnosis: '微信环境下 jsapi-config 请求失败，可能是接口不存在、签名域名未配置或微信公众号配置异常。',
      errorCode: failedConfig.loadErrorCode,
      message: failedConfig.loadErrorMessage,
      requestUrl: '/wechat/jsapi-config',
      requestMethod: 'GET',
      requestParams: { url: pageUrl },
      responseStatus: error?.response?.status ?? null,
      rawData: {
        responseData: error?.response?.data || null,
        stack: error?.stack || ''
      }
    })
    console.warn('[wechat jsapi config load failed]', error)
    return failedConfig
  }
}

function resolveBrowserLocationErrorCode(errorCode) {
  if (errorCode === 'UNSUPPORTED') {
    return 'BROWSER_GEO_UNSUPPORTED'
  }
  if (errorCode === 'UNAVAILABLE') {
    return 'BROWSER_GEO_UNAVAILABLE'
  }
  if (Number(errorCode) === 1) {
    return 'BROWSER_GEO_PERMISSION_DENIED'
  }
  if (Number(errorCode) === 2) {
    return 'BROWSER_GEO_POSITION_UNAVAILABLE'
  }
  if (Number(errorCode) === 3) {
    return 'BROWSER_GEO_TIMEOUT'
  }
  return 'BROWSER_GEO_FAILED'
}

function resolveWechatLocationErrorCode(error) {
  const rawMessage = String(error?.detail?.errMsg || error?.message || '').toLowerCase()
  if (rawMessage.includes('invalid signature')) {
    return 'WECHAT_JSAPI_INVALID_SIGNATURE'
  }
  if (error?.code === 'TIMEOUT') {
    return 'WECHAT_JSAPI_TIMEOUT'
  }
  if (error?.code === 'CANCEL') {
    return 'WECHAT_JSAPI_CANCEL'
  }
  if (error?.code === 'FAIL') {
    return 'WECHAT_JSAPI_FAIL'
  }
  return 'WECHAT_JSAPI_LOCATION_ERROR'
}

function buildLocationFailurePayload(reason, status, diagnostics = null, decisionBranch = 'INVALID', logMeta = {}) {
  return {
    reason,
    status,
    visualization: {
      ...(diagnostics || {}),
      error: reason,
      decisionBranch,
      overlap: false,
      weakToleranceApplied: false,
      stageText: '未获取到可用定位',
      hasUsableLocation: false,
      sampleTimestamp: diagnostics?.timestamp ?? null
    },
    logMeta: {
      errorCode: logMeta.errorCode || status || '',
      locationSource: logMeta.locationSource || '',
      provider: logMeta.provider || '',
      stage: logMeta.stage || decisionBranch,
      latitude: diagnostics?.submitLatitude ?? diagnostics?.rawLatitude ?? null,
      longitude: diagnostics?.submitLongitude ?? diagnostics?.rawLongitude ?? null,
      accuracy: diagnostics?.accuracyMeters ?? null,
      distanceMeters: diagnostics?.localDistanceMeters ?? null,
      radiusMeters: diagnostics?.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
      fallbackEnabled: Boolean(logMeta.fallbackEnabled),
      rawMessage: logMeta.rawMessage || '',
      suggestion: logMeta.suggestion || ''
    }
  }
}

async function applyTerminalLocationFailure(failure) {
  if (!failure) {
    return
  }
  state.checkInResult.success = false
  state.checkInResult.allowCheckIn = false
  state.checkInResult.action = ''
  state.checkInResult.status = failure.status || ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
  state.checkInResult.distanceMeters = null
  state.checkInResult.reason = failure.reason || '未获取到可用定位'
  updateCheckInVisualization(failure.visualization || {
    error: state.checkInResult.reason,
    stageText: '未获取到可用定位',
    hasUsableLocation: false
  })
  const environment = buildLocationEnvironmentDiagnostics()
  console.warn('[attendance check-in location failure]', {
    errorCode: failure?.logMeta?.errorCode || failure.status || ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED,
    locationSource: failure?.logMeta?.locationSource || '',
    provider: failure?.logMeta?.provider || '',
    stage: failure?.logMeta?.stage || '',
    latitude: failure?.logMeta?.latitude ?? null,
    longitude: failure?.logMeta?.longitude ?? null,
    accuracy: failure?.logMeta?.accuracy ?? null,
    distanceMeters: failure?.logMeta?.distanceMeters ?? null,
    radiusMeters: failure?.logMeta?.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
    fallbackEnabled: Boolean(failure?.logMeta?.fallbackEnabled),
    rawMessage: failure?.logMeta?.rawMessage || '',
    currentUrl: environment.url || '',
    userAgent: environment.userAgent || '',
    isWeChatEnv: isWechatBrowser()
  })
  await reportLog(
    failure?.logMeta?.provider === 'WECHAT' ? LOG_TYPES.WECHAT_JSAPI_ERROR : LOG_TYPES.FRONTEND_LOCATION_ERROR,
    {
      module: 'ATTENDANCE',
      subModule: 'CHECK_IN',
      title: failure?.logMeta?.provider === 'WECHAT' ? '微信定位失败' : '考勤定位失败',
      summary: state.checkInResult.reason,
      diagnosis: failure?.logMeta?.suggestion || '定位链路执行失败，请结合环境、定位权限与原始日志排查。',
      errorCode: failure?.logMeta?.errorCode || failure.status || '',
      message: failure?.logMeta?.rawMessage || state.checkInResult.reason,
      requestUrl: '/attendance/check-in',
      requestMethod: 'CHECK_IN',
      rawData: {
        visualization: failure.visualization || null,
        diagnostics: failure.visualization || null
      },
      ...buildLocationDiagnosticPayload({
        env: environment.userAgent && /micromessenger/i.test(environment.userAgent) ? 'WECHAT' : 'BROWSER',
        provider: failure?.logMeta?.locationSource || '',
        stage: failure?.logMeta?.stage || '',
        errorCode: failure?.logMeta?.errorCode || '',
        rawMessage: failure?.logMeta?.rawMessage || '',
        diagnostics: failure?.visualization || null,
        policy: locationAccuracyPolicy.value,
        extra: {
          fallbackEnabled: Boolean(failure?.logMeta?.fallbackEnabled),
          reason: state.checkInResult.reason
        }
      })
    }
  )
  showToast(state.checkInResult.reason)
}

async function tryCollectBrowserLocationSelection() {
  try {
    const geolocation = typeof navigator === 'undefined' ? null : navigator.geolocation
    const collected = await getUserLocation({
      geolocation,
      targetLatitude: state.locationInfo.latitude,
      targetLongitude: state.locationInfo.longitude,
      radiusMeters: state.locationInfo.radiusMeters,
      sampleCount: GEOLOCATION_SAMPLE_COUNT,
      timeoutMs: GEOLOCATION_TIMEOUT_MS
    })

    if (!collected.diagnostics) {
      const diagnostics = collected.samples[collected.samples.length - 1] || null
      const browserErrorCode = resolveBrowserLocationErrorCode(collected.errorCode ?? collected.lastError?.code)
      const invalidReason = resolveLocationFailureMessage({
        errorCode: browserErrorCode,
        accuracyMeters: diagnostics?.accuracyMeters ?? null,
        policy: locationAccuracyPolicy.value
      })
      return {
        failure: buildLocationFailurePayload(
          invalidReason,
          collected.errorCode === 'UNSUPPORTED'
            ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
            : ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID,
          diagnostics,
          collected.errorCode === 'UNSUPPORTED' ? 'LOCATION_UNSUPPORTED' : 'INVALID',
          {
            errorCode: browserErrorCode,
            locationSource: 'BROWSER_GEO',
            provider: 'BROWSER',
            stage: 'BROWSER_GEOLOCATION',
            rawMessage: collected.lastError?.message || collected.errorMessage || '',
            suggestion: buildLocationDiagnosticPayload({
              env: 'BROWSER',
              provider: 'BROWSER_GEO',
              stage: 'BROWSER_GEOLOCATION',
              errorCode: browserErrorCode,
              rawMessage: collected.lastError?.message || collected.errorMessage || '',
              diagnostics,
              policy: locationAccuracyPolicy.value
            }).suggestion
          }
        )
      }
    }
    const accuracyState = classifyLocationAccuracy(collected.diagnostics.accuracyMeters, locationAccuracyPolicy.value)
    if (accuracyState.category === 'INVALID' || accuracyState.category === 'WEAK') {
      const errorCode = accuracyState.category === 'INVALID'
        ? 'BROWSER_GEO_ACCURACY_INVALID'
        : 'BROWSER_GEO_ACCURACY_WEAK'
      const reason = accuracyState.category === 'INVALID'
        ? buildInvalidLocationReason(collected.diagnostics.accuracyMeters)
        : buildWeakLocationReason(collected.diagnostics.accuracyMeters)
      return {
        failure: buildLocationFailurePayload(
          reason,
          accuracyState.category === 'INVALID'
            ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
            : ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK,
          collected.diagnostics,
          accuracyState.category === 'INVALID' ? 'BROWSER_LOCATION_INVALID' : 'BROWSER_LOCATION_WEAK',
          {
            errorCode,
            locationSource: 'BROWSER_GEO',
            provider: 'BROWSER',
            stage: 'BROWSER_GEOLOCATION',
            rawMessage: '',
            suggestion: buildLocationDiagnosticPayload({
              env: 'BROWSER',
              provider: 'BROWSER_GEO',
              stage: 'BROWSER_GEOLOCATION',
              errorCode,
              diagnostics: collected.diagnostics,
              policy: locationAccuracyPolicy.value
            }).suggestion
          }
        )
      }
    }

    return {
      selection: {
        ...collected,
        source: 'BROWSER_GEO',
        provider: 'BROWSER'
      }
    }
  } catch (error) {
    const browserErrorCode = resolveBrowserLocationErrorCode(error?.code)
    return {
      failure: buildLocationFailurePayload(
        resolveLocationFailureMessage({
          errorCode: browserErrorCode,
          policy: locationAccuracyPolicy.value
        }),
        error?.code === 1 ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID : ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED,
        null,
        error?.code === 1 ? 'INVALID' : 'LOCATION_ERROR',
        {
          errorCode: browserErrorCode,
          locationSource: 'BROWSER_GEO',
          provider: 'BROWSER',
          stage: 'BROWSER_GEOLOCATION',
          rawMessage: error?.message || String(error || ''),
          suggestion: buildLocationDiagnosticPayload({
            env: 'BROWSER',
            provider: 'BROWSER_GEO',
            stage: 'BROWSER_GEOLOCATION',
            errorCode: browserErrorCode,
            rawMessage: error?.message || String(error || ''),
            policy: locationAccuracyPolicy.value
          }).suggestion
        }
      )
    }
  }
}

async function tryCollectWechatLocationSelection(jsapiConfig) {
  try {
    const result = await getWechatLocationByJsapi({
      config: jsapiConfig,
      timeoutMs: jsapiConfig.timeoutMs,
      locationType: jsapiConfig.locationType
    })
    const diagnostics = buildCheckInDiagnosticsFromCoordinates({
      latitude: result.latitude,
      longitude: result.longitude,
      accuracyMeters: result.accuracy,
      timestamp: Date.now(),
      coordinateType: jsapiConfig.locationType,
      targetLatitude: state.locationInfo.latitude,
      targetLongitude: state.locationInfo.longitude,
      radiusMeters: state.locationInfo.radiusMeters
    })
    const accuracyThreshold = Number.isFinite(jsapiConfig.accuracyThreshold)
      ? Number(jsapiConfig.accuracyThreshold)
      : locationAccuracyPolicy.value.goodThreshold
    const accuracyState = classifyLocationAccuracy(diagnostics.accuracyMeters, locationAccuracyPolicy.value)
    if (accuracyState.category === 'INVALID' || accuracyState.category === 'WEAK' || (accuracyThreshold != null && diagnostics.accuracyMeters != null && diagnostics.accuracyMeters > accuracyThreshold)) {
      const errorCode = accuracyState.category === 'INVALID'
        ? 'WECHAT_JSAPI_LOCATION_INVALID'
        : 'WECHAT_JSAPI_LOCATION_WEAK'
      return {
        failure: buildLocationFailurePayload(
          accuracyState.category === 'INVALID'
            ? buildInvalidLocationReason(diagnostics?.accuracyMeters)
            : buildWeakLocationReason(diagnostics?.accuracyMeters),
          accuracyState.category === 'INVALID'
            ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
            : ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK,
          diagnostics,
          accuracyState.category === 'INVALID' ? 'WECHAT_LOCATION_INVALID' : 'WECHAT_LOCATION_WEAK',
          {
            errorCode,
            locationSource: 'WECHAT_JSAPI',
            provider: 'WECHAT',
            stage: 'WECHAT_JSAPI_LOCATION',
            rawMessage: '',
            suggestion: buildLocationDiagnosticPayload({
              env: 'WECHAT',
              provider: 'WECHAT_JSAPI',
              stage: 'WECHAT_JSAPI_LOCATION',
              errorCode,
              diagnostics,
              policy: locationAccuracyPolicy.value
            }).suggestion
          }
        )
      }
    }
    return {
      selection: {
        latitude: diagnostics.submitLatitude,
        longitude: diagnostics.submitLongitude,
        accuracy: diagnostics.accuracyMeters,
        errorCode: null,
        errorMessage: '',
        diagnostics,
        source: 'WECHAT_JSAPI',
        provider: 'WECHAT'
      }
    }
  } catch (error) {
    const wechatErrorCode = resolveWechatLocationErrorCode(error)
    return {
      failure: buildLocationFailurePayload(
        resolveLocationFailureMessage({
          errorCode: wechatErrorCode,
          policy: locationAccuracyPolicy.value
        }),
        error?.code === 'CANCEL'
          ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
          : ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED,
        null,
        'WECHAT_LOCATION_ERROR',
        {
          errorCode: wechatErrorCode,
          locationSource: 'WECHAT_JSAPI',
          provider: 'WECHAT',
          stage: 'WECHAT_JSAPI_LOCATION',
          rawMessage: error?.detail?.errMsg || error?.message || '',
          suggestion: buildLocationDiagnosticPayload({
            env: 'WECHAT',
            provider: 'WECHAT_JSAPI',
            stage: 'WECHAT_JSAPI_LOCATION',
            errorCode: wechatErrorCode,
            rawMessage: error?.detail?.errMsg || error?.message || '',
            policy: locationAccuracyPolicy.value
          }).suggestion
        }
      )
    }
  }
}

async function resolveLocationSelectionOrFail() {
  console.info('[attendance geolocation environment]', buildLocationEnvironmentDiagnostics())
  const jsapiConfig = await queryWechatJsapiConfigForCheckIn()
  const inWechat = isWechatBrowser()
  const allowBrowserFallback = jsapiConfig.fallback === 'BROWSER'
  if (inWechat && jsapiConfig.loadFailed) {
    showToast(allowBrowserFallback ? '微信定位配置获取失败，已尝试切换浏览器定位' : '微信定位配置获取失败，请联系管理员检查签名与接口')
  }
  const canUseWechatLocation = inWechat
    && jsapiConfig.enabled
    && jsapiConfig.locationEnabled
    && jsapiConfig.jsApiList.includes('getLocation')
  const priority = canUseWechatLocation ? jsapiConfig.priority : WECHAT_JSAPI_DEFAULT_PRIORITY

  if (!inWechat && jsapiConfig.enabled && !allowBrowserFallback) {
    await applyTerminalLocationFailure(buildLocationFailurePayload(
      '当前不是微信环境，且系统未开启浏览器定位兜底',
      ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED,
      null,
      'NON_WECHAT_BLOCKED',
      {
        errorCode: 'NON_WECHAT_BLOCKED',
        locationSource: 'WECHAT_JSAPI',
        provider: 'WECHAT',
        stage: 'LOCATION_SELECTION',
        rawMessage: 'Current browser is not WeChat and browser fallback is disabled.',
        suggestion: buildLocationDiagnosticPayload({
          env: 'BROWSER',
          provider: 'WECHAT_JSAPI',
          stage: 'LOCATION_SELECTION',
          errorCode: 'NON_WECHAT_BLOCKED',
          rawMessage: 'Current browser is not WeChat and browser fallback is disabled.',
          policy: locationAccuracyPolicy.value
        }).suggestion
      }
    ))
    return null
  }

  if (!canUseWechatLocation) {
    const browserResult = await tryCollectBrowserLocationSelection()
    if (browserResult.failure) {
      await applyTerminalLocationFailure(browserResult.failure)
      return null
    }
    return browserResult.selection
  }

  if (priority === 'WECHAT_ONLY') {
    const wechatResult = await tryCollectWechatLocationSelection(jsapiConfig)
    if (wechatResult.failure) {
      await applyTerminalLocationFailure(wechatResult.failure)
      return null
    }
    return wechatResult.selection
  }

  if (priority === 'BROWSER_FIRST') {
    const browserResult = await tryCollectBrowserLocationSelection()
    if (browserResult.selection) {
      return browserResult.selection
    }
    const wechatResult = await tryCollectWechatLocationSelection(jsapiConfig)
    if (wechatResult.failure) {
      await applyTerminalLocationFailure(wechatResult.failure)
      return null
    }
    return wechatResult.selection
  }

  const wechatResult = await tryCollectWechatLocationSelection(jsapiConfig)
  if (wechatResult.selection) {
    return wechatResult.selection
  }
  if (allowBrowserFallback) {
    const browserResult = await tryCollectBrowserLocationSelection()
    if (browserResult.selection) {
      showToast('微信定位失败，已切换浏览器定位')
      return browserResult.selection
    }
    if (browserResult.failure) {
      await applyTerminalLocationFailure(browserResult.failure)
      return null
    }
  }
  await applyTerminalLocationFailure(wechatResult.failure)
  return null
}

function buildCheckInSubmissionPayload(locationSelection) {
  const usingWechatJsapi = locationSelection?.source === 'WECHAT_JSAPI'
  return {
    address: `${usingWechatJsapi ? '微信定位' : '浏览器定位'}：${locationSelection.latitude}, ${locationSelection.longitude}`,
    latitude: locationSelection.latitude,
    longitude: locationSelection.longitude,
    accuracyMeters: locationSelection.accuracy ?? null,
    locationSource: locationSelection.source || 'BROWSER_GEO',
    locationProvider: locationSelection.provider || 'BROWSER'
  }
}

function updateCheckInVisualization(payload = {}) {
  state.checkInVisualization.rawLatitude = payload.rawLatitude ?? state.checkInVisualization.rawLatitude ?? null
  state.checkInVisualization.rawLongitude = payload.rawLongitude ?? state.checkInVisualization.rawLongitude ?? null
  state.checkInVisualization.convertedLatitude = payload.convertedLatitude ?? state.checkInVisualization.convertedLatitude ?? null
  state.checkInVisualization.convertedLongitude = payload.convertedLongitude ?? state.checkInVisualization.convertedLongitude ?? null
  state.checkInVisualization.submitLatitude = payload.submitLatitude ?? state.checkInVisualization.submitLatitude ?? null
  state.checkInVisualization.submitLongitude = payload.submitLongitude ?? state.checkInVisualization.submitLongitude ?? null
  state.checkInVisualization.targetLatitude = payload.targetLatitude ?? state.locationInfo.latitude ?? null
  state.checkInVisualization.targetLongitude = payload.targetLongitude ?? state.locationInfo.longitude ?? null
  state.checkInVisualization.radiusMeters = payload.radiusMeters ?? state.locationInfo.radiusMeters ?? null
  state.checkInVisualization.accuracyMeters = payload.accuracyMeters ?? state.checkInVisualization.accuracyMeters ?? null
  state.checkInVisualization.localDistanceMeters = payload.localDistanceMeters ?? state.checkInVisualization.localDistanceMeters ?? null
  state.checkInVisualization.overlap = payload.overlap == null ? Boolean(state.checkInVisualization.overlap) : Boolean(payload.overlap)
  state.checkInVisualization.decisionBranch = payload.decisionBranch ?? state.checkInVisualization.decisionBranch ?? ''
  state.checkInVisualization.weakToleranceApplied = payload.weakToleranceApplied == null ? Boolean(state.checkInVisualization.weakToleranceApplied) : Boolean(payload.weakToleranceApplied)
  state.checkInVisualization.error = payload.error ?? ''
  state.checkInVisualization.stageText = payload.stageText ?? state.checkInVisualization.stageText ?? ''
  state.checkInVisualization.hasUsableLocation = payload.hasUsableLocation == null ? Boolean(state.checkInVisualization.hasUsableLocation) : Boolean(payload.hasUsableLocation)
  state.checkInVisualization.sampleTimestamp = payload.sampleTimestamp ?? state.checkInVisualization.sampleTimestamp ?? null
}

function resetCheckInVisualizationTarget() {
  state.checkInVisualization.targetLatitude = state.locationInfo.latitude ?? null
  state.checkInVisualization.targetLongitude = state.locationInfo.longitude ?? null
  state.checkInVisualization.radiusMeters = state.locationInfo.radiusMeters ?? null
}

async function ensureCheckInMapReady() {
  if (!checkInMapRef.value) {
    return null
  }
  if (checkInMapInstance && checkInAmap) {
    return checkInMapInstance
  }
  try {
    checkInAmap = await loadAmapSdk()
    if (!checkInMapRef.value) {
      return null
    }
    checkInMapInstance = new checkInAmap.Map(checkInMapRef.value, {
      zoom: CHECK_IN_MAP_DEFAULT_ZOOM,
      center: [118.091519, 24.478829],
      mapStyle: 'amap://styles/normal',
      viewMode: '2D'
    })
    checkInMapElements = {
      targetMarker: new checkInAmap.Marker({
        anchor: 'bottom-center',
        label: { content: '打卡点', direction: 'top' }
      }),
      currentMarker: new checkInAmap.Marker({
        anchor: 'bottom-center',
        label: { content: '当前位置', direction: 'top' }
      }),
      targetCircle: new checkInAmap.Circle({
        strokeColor: CHECK_IN_MAP_COLORS.target,
        strokeOpacity: 0.9,
        strokeWeight: 2,
        fillColor: CHECK_IN_MAP_COLORS.target,
        fillOpacity: 0.08
      }),
      currentCircle: new checkInAmap.Circle({
        strokeColor: CHECK_IN_MAP_COLORS.current,
        strokeOpacity: 0.9,
        strokeWeight: 2,
        fillColor: CHECK_IN_MAP_COLORS.current,
        fillOpacity: 0.08
      }),
      line: new checkInAmap.Polyline({
        strokeColor: CHECK_IN_MAP_COLORS.line,
        strokeWeight: 3,
        strokeOpacity: 0.85
      })
    }
    checkInMapInstance.add([
      checkInMapElements.targetCircle,
      checkInMapElements.currentCircle,
      checkInMapElements.line,
      checkInMapElements.targetMarker,
      checkInMapElements.currentMarker
    ])
    return checkInMapInstance
  } catch (error) {
    state.checkInVisualization.error = error.message || '定位地图加载失败'
    return null
  }
}

function toggleCheckInOverlay(overlay, visible) {
  if (!checkInMapInstance || !overlay) {
    return
  }
  if (visible) {
    checkInMapInstance.add(overlay)
  } else {
    checkInMapInstance.remove(overlay)
  }
}

async function syncCheckInMap() {
  await nextTick()
  const instance = await ensureCheckInMapReady()
  if (!instance || !checkInMapElements) {
    return
  }
  const targetReady = state.locationInfo.latitude != null && state.locationInfo.longitude != null
  const currentReady = state.checkInVisualization.submitLatitude != null && state.checkInVisualization.submitLongitude != null
  const fitTargets = []

  if (targetReady) {
    const targetCenter = [Number(state.locationInfo.longitude), Number(state.locationInfo.latitude)]
    checkInMapElements.targetMarker.setPosition(targetCenter)
    checkInMapElements.targetCircle.setCenter(targetCenter)
    checkInMapElements.targetCircle.setRadius(Number(state.locationInfo.radiusMeters || 0))
    toggleCheckInOverlay(checkInMapElements.targetMarker, true)
    toggleCheckInOverlay(checkInMapElements.targetCircle, true)
    fitTargets.push(checkInMapElements.targetMarker, checkInMapElements.targetCircle)
  } else {
    toggleCheckInOverlay(checkInMapElements.targetMarker, false)
    toggleCheckInOverlay(checkInMapElements.targetCircle, false)
  }

  if (currentReady) {
    const currentCenter = [Number(state.checkInVisualization.submitLongitude), Number(state.checkInVisualization.submitLatitude)]
    checkInMapElements.currentMarker.setPosition(currentCenter)
    checkInMapElements.currentCircle.setCenter(currentCenter)
    checkInMapElements.currentCircle.setRadius(Number(state.checkInVisualization.accuracyMeters || 0))
    toggleCheckInOverlay(checkInMapElements.currentMarker, true)
    toggleCheckInOverlay(checkInMapElements.currentCircle, true)
    fitTargets.push(checkInMapElements.currentMarker, checkInMapElements.currentCircle)
    if (targetReady) {
      checkInMapElements.line.setPath([
        [Number(state.locationInfo.longitude), Number(state.locationInfo.latitude)],
        currentCenter
      ])
      toggleCheckInOverlay(checkInMapElements.line, true)
      fitTargets.push(checkInMapElements.line)
    } else {
      toggleCheckInOverlay(checkInMapElements.line, false)
    }
  } else {
    toggleCheckInOverlay(checkInMapElements.currentMarker, false)
    toggleCheckInOverlay(checkInMapElements.currentCircle, false)
    toggleCheckInOverlay(checkInMapElements.line, false)
  }

  if (fitTargets.length) {
    instance.setFitView(fitTargets, false, [28, 28, 28, 28])
  }
}

function riskLevelLabel(level) {
  if (level === 'HIGH') {
    return '高风险'
  }
  if (level === 'MEDIUM') {
    return '中风险'
  }
  if (level === 'LOW') {
    return '低风险'
  }
  return '-'
}

function riskBadgeClass(level) {
  if (level === 'HIGH') {
    return 'rank-badge-warning'
  }
  if (level === 'MEDIUM') {
    return 'rank-badge-primary'
  }
  return ''
}

function trendDirectionLabel(direction) {
  if (direction === 'RISING') {
    return '上升'
  }
  if (direction === 'FALLING') {
    return '下降'
  }
  if (direction === 'STABLE') {
    return '持平'
  }
  return '-'
}

function normalizeFormSelectedUser(user) {
  if (!user) {
    return null
  }
  return {
    id: user.id ?? null,
    realName: user.realName || '',
    username: user.username || '',
    mobile: user.mobile || '',
    unitName: user.unitName || ''
  }
}

function resultLabel(status) {
  return getAttendanceStatusLabel(status)
}

function resultTagType(status) {
  return getAttendanceStatusTagType(status)
}

function isAbnormalStatus(status) {
  return status && status !== ATTENDANCE_CHECK_IN_STATUS.CHECK_IN_SUCCESS && status !== ATTENDANCE_CHECK_IN_STATUS.CHECK_OUT_SUCCESS
}

function buildQueryPayload() {
  return {
    keywords: state.queryForm.keywords || undefined,
    unitName: state.queryForm.unitName || undefined,
    checkInStatus: state.queryForm.checkInStatus || undefined,
    userId: state.queryForm.userId || undefined,
    abnormalOnly: state.queryForm.abnormalOnly || undefined,
    dateFrom: state.queryForm.dateFrom || undefined,
    dateTo: state.queryForm.dateTo || undefined,
    pageNo: state.pageNo,
    pageSize: state.pageSize
  }
}

function buildTrendPayload() {
  return {
    userId: state.queryForm.userId || undefined,
    unitName: state.queryForm.unitName || undefined,
    dateFrom: state.trendDateLinkage.selectedDate ? state.trendDateLinkage.backupDateFrom || undefined : state.queryForm.dateFrom || undefined,
    dateTo: state.trendDateLinkage.selectedDate ? state.trendDateLinkage.backupDateTo || undefined : state.queryForm.dateTo || undefined
  }
}

function buildAbnormalMonitorPayload() {
  const trendPayload = buildTrendPayload()
  return {
    keywords: state.queryForm.keywords || undefined,
    unitName: state.queryForm.unitName || undefined,
    checkInStatus: state.queryForm.checkInStatus || undefined,
    userId: state.queryForm.userId || undefined,
    abnormalOnly: state.queryForm.abnormalOnly || undefined,
    dateFrom: trendPayload.dateFrom,
    dateTo: trendPayload.dateTo
  }
}

function formatRangeText(dateFrom, dateTo) {
  if (dateFrom && dateTo) {
    return `${dateFrom} 至 ${dateTo}`
  }
  if (dateFrom) {
    return `${dateFrom} 起`
  }
  if (dateTo) {
    return `截至 ${dateTo}`
  }
  return '全部时间'
}

function validateQueryDateRange() {
  if (state.queryForm.dateFrom && state.queryForm.dateTo && state.queryForm.dateFrom > state.queryForm.dateTo) {
    showToast('开始日期不能晚于结束日期')
    return false
  }
  return true
}

function isQuickRangeActive(key) {
  const range = getQuickRangeDates(key)
  return state.queryForm.dateFrom === range.dateFrom && state.queryForm.dateTo === range.dateTo
}

function clearTrendDateLinkage(options = {}) {
  const { restoreDates = false } = options
  if (restoreDates) {
    state.queryForm.dateFrom = state.trendDateLinkage.backupDateFrom || ''
    state.queryForm.dateTo = state.trendDateLinkage.backupDateTo || ''
  }
  state.trendDateLinkage.selectedDate = ''
  state.trendDateLinkage.backupDateFrom = ''
  state.trendDateLinkage.backupDateTo = ''
}

const locationGuide = computed(() => {
  if (isDesktopLocationEnvironment()) {
    return buildDesktopLocationHint()
  }
  if (state.locationInfo.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_NOT_CONFIGURED) {
    return '所属单位尚未配置打卡点，请联系管理员前往“单位管理 > 打卡点设置”完成配置。'
  }
  if (state.locationInfo.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_DISABLED) {
    return '当前单位打卡点未启用，请联系管理员前往“单位管理 > 打卡点设置”启用后再打卡。'
  }
  return ''
})

const abnormalMonitorRangeText = computed(() => {
  const payload = buildAbnormalMonitorPayload()
  return formatRangeText(payload.dateFrom, payload.dateTo)
})

const trendRangeText = computed(() => {
  const payload = buildTrendPayload()
  return formatRangeText(payload.dateFrom, payload.dateTo)
})

const trendLinkageHint = computed(() => {
  if (!state.trendDateLinkage.selectedDate) {
    return ''
  }
  return `当前明细已切到 ${state.trendDateLinkage.selectedDate} 的当天异常记录，趋势、摘要与总览仍按 ${trendRangeText.value} 统计。`
})

const selectedAbnormalUserName = computed(() => {
  const current = state.abnormalMonitor.topUsers.find((item) => item.userId === state.abnormalSelection.activeUserId) || state.trendUser
  if (!current) {
    return '-'
  }
  return current.realName || current.username || `用户${current.userId}`
})

const listLinkageSummary = computed(() => {
  const parts = []
  if (state.abnormalSelection.activeUserId) {
    parts.push(`异常用户 ${selectedAbnormalUserName.value}`)
  }
  if (state.trendDateLinkage.selectedDate) {
    parts.push(`当天异常 ${state.trendDateLinkage.selectedDate}`)
  }
  return parts.join(' / ')
})

const listRangeText = computed(() => {
  return formatRangeText(state.queryForm.dateFrom || undefined, state.queryForm.dateTo || undefined)
})

const activeQuickRangeLabel = computed(() => {
  const matched = QUICK_RANGE_OPTIONS.find((item) => isQuickRangeActive(item.key))
  return matched ? matched.label : '自定义'
})

const listPageSummary = computed(() => {
  if (!state.total || !state.list.length) {
    return ''
  }
  const start = (state.pageNo - 1) * state.pageSize + 1
  const end = start + state.list.length - 1
  return `当前第 ${state.pageNo} 页，展示第 ${start}-${end} 条`
})

const listFilterSummary = computed(() => {
  const parts = []
  if (state.queryForm.abnormalOnly) {
    parts.push('仅看异常记录')
  }
  if (state.queryForm.userId) {
    parts.push(`用户 ${selectedAbnormalUserName.value}`)
  }
  if (state.queryForm.keywords) {
    parts.push(`关键词 ${state.queryForm.keywords}`)
  }
  if (state.queryForm.unitName) {
    parts.push(`组织 ${state.queryForm.unitName}`)
  }
  return parts.join(' / ')
})

const hasResettableQueryConditions = computed(() => {
  return Boolean(
    state.queryForm.keywords ||
    state.queryForm.unitName ||
    state.queryForm.checkInStatus ||
    state.queryForm.userId ||
    state.queryForm.abnormalOnly ||
    state.queryForm.dateFrom ||
    state.queryForm.dateTo ||
    state.abnormalSelection.activeUserId ||
    state.trendDateLinkage.selectedDate
  )
})

const checkInCurrentCoordinateText = computed(() => {
  if (state.checkInVisualization.submitLatitude == null || state.checkInVisualization.submitLongitude == null) {
    return '-'
  }
  return `${formatCoordinate(state.checkInVisualization.submitLatitude)}, ${formatCoordinate(state.checkInVisualization.submitLongitude)}`
})

const checkInTargetCoordinateText = computed(() => {
  if (state.locationInfo.latitude == null || state.locationInfo.longitude == null) {
    return '-'
  }
  return `${formatCoordinate(state.locationInfo.latitude)}, ${formatCoordinate(state.locationInfo.longitude)}`
})

const checkInAllowedRadiusText = computed(() => state.locationInfo.radiusMeters == null ? '-' : `${state.locationInfo.radiusMeters}米`)
const checkInAccuracyText = computed(() => state.checkInVisualization.accuracyMeters == null ? '-' : `${state.checkInVisualization.accuracyMeters}米`)
const checkInDistanceText = computed(() => state.checkInVisualization.localDistanceMeters == null ? '-' : `${state.checkInVisualization.localDistanceMeters}米`)
const checkInOverlapText = computed(() => state.checkInVisualization.submitLatitude == null ? '-' : (state.checkInVisualization.overlap ? '是' : '否'))
const checkInDecisionText = computed(() => state.checkInResult.status || state.checkInVisualization.decisionBranch || '-')

async function fetchList() {
  if (!validateQueryDateRange()) {
    return
  }
  state.loading = true
  state.summaryLoading = true
  state.abnormalLoading = true
  state.trendLoading = Boolean(state.queryForm.userId)
  state.userSummaryLoading = Boolean(state.queryForm.userId)
  try {
    const payload = buildQueryPayload()
    const abnormalMonitorPayload = buildAbnormalMonitorPayload()
    const requests = [
      queryAttendanceListApi(payload),
      queryAttendanceSummaryApi(payload),
      queryAttendanceAbnormalMonitorApi(abnormalMonitorPayload)
    ]
    if (state.queryForm.userId) {
      const trendPayload = buildTrendPayload()
      requests.push(queryAttendanceAbnormalTrendApi(trendPayload))
      requests.push(
        queryAttendanceAbnormalUserSummaryApi({
          userId: state.queryForm.userId,
          unitName: state.queryForm.unitName || undefined,
          dateFrom: trendPayload.dateFrom,
          dateTo: trendPayload.dateTo
        })
      )
    }
    const [response, summaryResponse, abnormalResponse, trendResponse, summaryUserResponse] = await Promise.all(requests)
    const pageData = response?.data || {}
    state.pageNo = Number(pageData.pageNo || state.pageNo || 1)
    state.pageSize = Number(pageData.pageSize || state.pageSize || 10)
    state.total = Number(pageData.total || 0)
    state.list = Array.isArray(pageData.list) ? pageData.list : []
    if (state.total > 0 && !state.list.length && state.pageNo > 1) {
      state.pageNo -= 1
      await fetchList()
      return
    }
    const summaryData = summaryResponse?.data || {}
    state.summary.totalCount = Number(summaryData.totalCount || 0)
    state.summary.successCount = Number(summaryData.successCount || 0)
    state.summary.abnormalCount = Number(summaryData.abnormalCount || 0)
    state.summary.statusCounts = Array.isArray(summaryData.statusCounts) ? summaryData.statusCounts : []
    const abnormalData = abnormalResponse?.data || {}
    state.abnormalMonitor.topUsers = Array.isArray(abnormalData.topUsers) ? abnormalData.topUsers : []
    state.abnormalMonitor.statusCounts = Array.isArray(abnormalData.statusCounts) ? abnormalData.statusCounts : []
    state.abnormalMonitor.reasonDistributions = Array.isArray(abnormalData.reasonDistributions) ? abnormalData.reasonDistributions : []
    state.abnormalMonitor.highRiskCount = Number(abnormalData.highRiskCount || 0)
    state.abnormalMonitor.alertCount = Number(abnormalData.alertCount || 0)
    state.abnormalTrend = Array.isArray(trendResponse?.data) ? trendResponse.data : []
    state.abnormalUserSummary = summaryUserResponse?.data || null
    if (state.queryForm.userId) {
      state.trendUser = state.abnormalMonitor.topUsers.find((item) => item.userId === state.queryForm.userId) || state.trendUser
    }
  } catch (error) {
    state.total = 0
    state.list = []
    state.summary.totalCount = 0
    state.summary.successCount = 0
    state.summary.abnormalCount = 0
    state.summary.statusCounts = []
    state.abnormalMonitor.topUsers = []
    state.abnormalMonitor.statusCounts = []
    state.abnormalMonitor.reasonDistributions = []
    state.abnormalMonitor.highRiskCount = 0
    state.abnormalMonitor.alertCount = 0
    state.abnormalTrend = []
    state.abnormalUserSummary = null
    showToast(error.message || '考勤列表加载失败')
  } finally {
    state.loading = false
    state.summaryLoading = false
    state.abnormalLoading = false
    state.trendLoading = false
    state.userSummaryLoading = false
  }
}

async function fetchCurrentLocation() {
  state.locationLoading = true
  try {
    const response = await queryCurrentAttendanceLocationApi()
    const data = response.data || {}
    state.locationInfo.unitName = data.unitName || ''
    state.locationInfo.locationName = data.locationName || ''
    state.locationInfo.address = data.address || ''
    state.locationInfo.latitude = data.latitude ?? null
    state.locationInfo.longitude = data.longitude ?? null
    state.locationInfo.radiusMeters = data.radiusMeters ?? null
    state.locationInfo.accuracyGoodThreshold = Number(data.accuracyGoodThreshold ?? 100)
    state.locationInfo.accuracyMaxThreshold = Number(data.accuracyMaxThreshold ?? 1000)
    state.locationInfo.allowCheckIn = Boolean(data.allowCheckIn)
    state.locationInfo.status = data.status || ''
    state.locationInfo.reason = data.reason || ''
    resetCheckInVisualizationTarget()
  } catch (error) {
    state.locationInfo.unitName = ''
    state.locationInfo.locationName = ''
    state.locationInfo.address = ''
    state.locationInfo.latitude = null
    state.locationInfo.longitude = null
    state.locationInfo.radiusMeters = null
    state.locationInfo.accuracyGoodThreshold = 100
    state.locationInfo.accuracyMaxThreshold = 1000
    state.locationInfo.allowCheckIn = false
    state.locationInfo.status = ''
    state.locationInfo.reason = error.message || '打卡点信息加载失败'
    resetCheckInVisualizationTarget()
  } finally {
    state.locationLoading = false
    syncCheckInMap()
  }
}

function handleSearch() {
  state.pageNo = 1
  clearTrendDateLinkage()
  fetchList()
}

function handleApplyQuickRange(rangeKey) {
  const range = getQuickRangeDates(rangeKey)
  state.queryForm.dateFrom = range.dateFrom
  state.queryForm.dateTo = range.dateTo
  state.pageNo = 1
  handleSearch()
}

function handleReset() {
  state.pageNo = 1
  state.queryForm.keywords = ''
  state.queryForm.unitName = ''
  state.queryForm.checkInStatus = ''
  state.queryForm.userId = null
  state.queryForm.abnormalOnly = false
  state.queryForm.dateFrom = ''
  state.queryForm.dateTo = ''
  state.abnormalTrend = []
  state.abnormalUserSummary = null
  state.trendUser = null
  clearTrendDateLinkage()
  fetchList()
}

function handleSelectAbnormalUser(item) {
  if (state.abnormalSelection.activeUserId === item.userId) {
    restoreAbnormalSelection()
    fetchList()
    showToast('已恢复异常用户联动前的筛选条件')
    return
  }
  if (state.trendDateLinkage.selectedDate) {
    clearTrendDateLinkage({ restoreDates: true })
  }
  if (!state.abnormalSelection.activeUserId) {
    state.abnormalSelection.backupKeywords = state.queryForm.keywords || ''
    state.abnormalSelection.backupUserId = state.queryForm.userId || null
    state.abnormalSelection.backupAbnormalOnly = Boolean(state.queryForm.abnormalOnly)
    state.abnormalSelection.backupCheckInStatus = state.queryForm.checkInStatus || ''
    state.abnormalSelection.backupDateFrom = state.queryForm.dateFrom || ''
    state.abnormalSelection.backupDateTo = state.queryForm.dateTo || ''
  }
  state.abnormalSelection.activeUserId = item.userId || null
  state.queryForm.userId = item.userId || null
  state.queryForm.keywords = item.realName || item.username || ''
  state.queryForm.abnormalOnly = true
  state.queryForm.checkInStatus = ''
  state.pageNo = 1
  state.trendUser = item
  fetchList()
  showToast(`已切换到 ${item.realName || item.username || item.userId} 的异常记录`)
}

function restoreAbnormalSelection() {
  clearTrendDateLinkage({ restoreDates: false })
  state.queryForm.keywords = state.abnormalSelection.backupKeywords || ''
  state.queryForm.userId = state.abnormalSelection.backupUserId || null
  state.queryForm.abnormalOnly = Boolean(state.abnormalSelection.backupAbnormalOnly)
  state.queryForm.checkInStatus = state.abnormalSelection.backupCheckInStatus || ''
  state.queryForm.dateFrom = state.abnormalSelection.backupDateFrom || ''
  state.queryForm.dateTo = state.abnormalSelection.backupDateTo || ''
  state.pageNo = 1
  state.abnormalSelection.activeUserId = null
  state.abnormalSelection.backupKeywords = ''
  state.abnormalSelection.backupUserId = null
  state.abnormalSelection.backupAbnormalOnly = false
  state.abnormalSelection.backupCheckInStatus = ''
  state.abnormalSelection.backupDateFrom = ''
  state.abnormalSelection.backupDateTo = ''
  state.abnormalTrend = []
  state.abnormalUserSummary = null
  state.trendUser = null
}

function handleClearAbnormalSelection() {
  if (!state.abnormalSelection.activeUserId && !state.trendDateLinkage.selectedDate) {
    return
  }
  restoreAbnormalSelection()
  fetchList()
  showToast('已恢复异常联动前的筛选条件')
}

async function handleTrendDateClick(item) {
  if (!state.queryForm.userId || !item?.attendanceDate) {
    return
  }
  if (state.trendDateLinkage.selectedDate === item.attendanceDate) {
    clearTrendDateLinkage({ restoreDates: true })
    await fetchList()
    showToast('已恢复原时间范围')
    return
  }
  if (!state.trendDateLinkage.selectedDate) {
    state.trendDateLinkage.backupDateFrom = state.queryForm.dateFrom || ''
    state.trendDateLinkage.backupDateTo = state.queryForm.dateTo || ''
  }
  state.trendDateLinkage.selectedDate = item.attendanceDate
  state.queryForm.dateFrom = item.attendanceDate
  state.queryForm.dateTo = item.attendanceDate
  state.queryForm.abnormalOnly = true
  state.pageNo = 1
  await fetchList()
  showToast(`已切换到 ${item.attendanceDate} 的当天异常明细`)
}

async function handleClearTrendDateLinkage() {
  if (!state.trendDateLinkage.selectedDate) {
    return
  }
  clearTrendDateLinkage({ restoreDates: true })
  state.pageNo = 1
  await fetchList()
  showToast('已清除当天联动')
}

function handlePageChange(pageNo) {
  if (pageBusy.value || Number(pageNo) === Number(state.pageNo)) {
    return
  }
  state.pageNo = Number(pageNo)
  fetchList()
}

function resetUserPicker() {
  state.userPicker.loading = false
  state.userPicker.searched = false
  state.userPicker.keyword = ''
  state.userPicker.candidates = []
}

function resetForm() {
  state.form = createEmptyForm()
  resetUserPicker()
}

async function handleSearchFormUsers() {
  const keywords = state.userPicker.keyword.trim()
  if (!keywords) {
    showToast('请输入姓名或手机号')
    return
  }
  state.userPicker.loading = true
  state.userPicker.searched = true
  try {
    const response = await queryUserPageApi({
      keywords,
      pageNo: 1,
      pageSize: 8
    })
    const page = response?.data || {}
    state.userPicker.candidates = Array.isArray(page.list) ? page.list : []
    if (!state.userPicker.candidates.length) {
      showToast('未找到匹配用户')
    }
  } catch (error) {
    state.userPicker.candidates = []
    showToast(error.message || '用户搜索失败')
  } finally {
    state.userPicker.loading = false
  }
}

function handleSelectFormUser(user) {
  state.form.userId = user?.id ? String(user.id) : ''
  state.form.selectedUser = normalizeFormSelectedUser(user)
  state.userPicker.keyword = user?.realName || user?.mobile || user?.username || ''
  showToast(`已选择 ${user?.realName || user?.username || user?.id}`)
}

function handleClearSelectedFormUser() {
  state.form.userId = ''
  state.form.selectedUser = null
  resetUserPicker()
}

function resolveCheckInRequestErrorMessage(error) {
  const backendMessage = error?.response?.data?.message
  const rawMessage = backendMessage && String(backendMessage).trim()
    ? String(backendMessage).trim()
    : (error?.message && String(error.message).trim() ? String(error.message).trim() : '')
  const inferredStatus = inferCheckInStatusFromErrorMessage(rawMessage)
  if (inferredStatus === ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE) {
    return buildOutOfRangeReason(
      state.checkInVisualization.localDistanceMeters,
      state.locationInfo.radiusMeters,
      state.checkInVisualization.accuracyMeters
    )
  }
  if (inferredStatus === ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED || inferredStatus === ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID) {
    return resolveLocationFailureMessage({
      errorCode: rawMessage.includes('超时') ? 'LOCATION_TIMEOUT' : 'LOCATION_INVALID',
      accuracyMeters: state.checkInVisualization.accuracyMeters,
      policy: locationAccuracyPolicy.value
    })
  }
  if (inferredStatus === ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK) {
    return buildWeakLocationReason(state.checkInVisualization.accuracyMeters)
  }
  return rawMessage || CHECK_IN_FAILURE_MESSAGE
}

function inferCheckInStatusFromErrorMessage(message) {
  if (!message) {
    return ''
  }
  if (message.includes('定位无效') || message.includes('无法获取当前位置') || message.includes('定位权限')) {
    return ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
  }
  if (message.includes('定位精度不足') || message.includes('定位质量较差') || message.includes('室内信号')) {
    return ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK
  }
  if (message.includes('未配置打卡点') || message.includes('配置不完整')) {
    return ATTENDANCE_CHECK_IN_STATUS.LOCATION_NOT_CONFIGURED
  }
  if (message.includes('打卡点未启用')) {
    return ATTENDANCE_CHECK_IN_STATUS.LOCATION_DISABLED
  }
  if (message.includes('未绑定单位')) {
    return ATTENDANCE_CHECK_IN_STATUS.LOCATION_NOT_BOUND
  }
  if (message.includes('范围') || message.includes('超出') || message.includes('距离打卡点')) {
    return ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE
  }
  if (message.includes('今日考勤已完成')) {
    return ATTENDANCE_CHECK_IN_STATUS.ALREADY_FINISHED
  }
  return ''
}

async function legacyHandleCheckInBrowserOnly() {
  state.checkingIn = true
  resetCheckInVisualizationSubmission()
  try {
    let locationSelection = null
    try {
      const geolocation = typeof navigator === 'undefined' ? null : navigator.geolocation
      console.info('[attendance geolocation environment]', buildLocationEnvironmentDiagnostics())
      updateCheckInVisualization({
        stageText: '正在获取定位...',
        hasUsableLocation: false,
        error: ''
      })

      const collected = await getUserLocation({
        geolocation,
        targetLatitude: state.locationInfo.latitude,
        targetLongitude: state.locationInfo.longitude,
        radiusMeters: state.locationInfo.radiusMeters,
        sampleCount: GEOLOCATION_SAMPLE_COUNT,
        timeoutMs: GEOLOCATION_TIMEOUT_MS
      })

      if (!collected.diagnostics) {
        const invalidReason = collected.invalidReason || (
          collected.errorCode === 'UNSUPPORTED'
            ? buildInvalidLocationReason(null)
            : buildInvalidLocationReason(collected.samples[collected.samples.length - 1]?.accuracyMeters ?? null)
        )
        const environment = buildLocationEnvironmentDiagnostics()
        updateCheckInVisualization({
          ...(collected.samples[collected.samples.length - 1] || {}),
          error: invalidReason,
          decisionBranch: collected.errorCode === 'UNSUPPORTED' ? 'LOCATION_UNSUPPORTED' : 'INVALID',
          overlap: false,
          weakToleranceApplied: false,
          stageText: '未获取到可用定位',
          hasUsableLocation: false,
          sampleTimestamp: collected.samples[collected.samples.length - 1]?.timestamp ?? null
        })
        console.warn('[attendance check-in location failure]', {
          errorCode: resolveBrowserLocationErrorCode(collected.errorCode ?? collected.lastError?.code),
          locationSource: 'BROWSER_GEO',
          provider: 'BROWSER',
          stage: 'LEGACY_BROWSER_GEOLOCATION',
          latitude: collected.samples[collected.samples.length - 1]?.submitLatitude ?? null,
          longitude: collected.samples[collected.samples.length - 1]?.submitLongitude ?? null,
          accuracy: collected.samples[collected.samples.length - 1]?.accuracyMeters ?? null,
          distanceMeters: collected.samples[collected.samples.length - 1]?.localDistanceMeters ?? null,
          radiusMeters: collected.samples[collected.samples.length - 1]?.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
          fallbackEnabled: false,
          rawMessage: collected.lastError?.message || collected.errorMessage || '',
          currentUrl: environment.url || '',
          userAgent: environment.userAgent || '',
          isWeChatEnv: isWechatBrowser()
        })
        state.checkInResult.success = false
        state.checkInResult.allowCheckIn = false
        state.checkInResult.action = ''
        state.checkInResult.status = collected.errorCode === 'UNSUPPORTED'
          ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
          : ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
        state.checkInResult.distanceMeters = null
        state.checkInResult.reason = invalidReason
        showToast(state.checkInResult.reason)
        return
      } else {
        locationSelection = {
          ...collected,
          source: 'BROWSER_GEO'
        }
      }
      if (!locationSelection) {
        return
      }
      const diagnostics = locationSelection.diagnostics
      updateCheckInVisualization({
        ...diagnostics,
        error: '',
        stageText: '正在提交打卡...',
        hasUsableLocation: true,
        sampleTimestamp: diagnostics.timestamp ?? null
      })
      const environment = buildLocationEnvironmentDiagnostics()
      console.info('[attendance check-in submit diagnostics]', {
        source: locationSelection.source,
        provider: locationSelection.provider || 'BROWSER',
        errorCode: locationSelection.errorCode || '',
        stage: 'LEGACY_CHECK_IN_SUBMIT',
        selectedTimestamp: diagnostics.timestamp,
        rawLatitude: diagnostics.rawLatitude,
        rawLongitude: diagnostics.rawLongitude,
        convertedLatitude: diagnostics.convertedLatitude,
        convertedLongitude: diagnostics.convertedLongitude,
        coordinateSource: diagnostics.coordinateSource,
        converted: diagnostics.coordinateSource === 'gcj02-adjusted',
        targetLatitude: diagnostics.targetLatitude,
        targetLongitude: diagnostics.targetLongitude,
        radiusMeters: diagnostics.radiusMeters,
        accuracyMeters: diagnostics.accuracyMeters,
        localDistanceMeters: diagnostics.localDistanceMeters,
        overlap: diagnostics.overlap,
        submitLatitude: diagnostics.submitLatitude,
        submitLongitude: diagnostics.submitLongitude,
        fallbackEnabled: false,
        currentUrl: environment.url || '',
        userAgent: environment.userAgent || '',
        isWeChatEnv: isWechatBrowser()
      })
      const response = await checkInApi(buildCheckInSubmissionPayload(locationSelection))
      const result = response.data || {}
      console.info('[attendance check-in response]', {
        ...diagnostics,
        responseDistanceMeters: result.distanceMeters ?? null,
        responseRadiusMeters: result.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
        responseStatus: result.status || '',
        responseReason: result.failReason || result.reason || ''
      })
      updateCheckInVisualization({
        ...diagnostics,
        localDistanceMeters: result.distanceMeters ?? diagnostics.localDistanceMeters,
        decisionBranch: result.decisionBranch || '',
        weakToleranceApplied: Boolean(result.weakToleranceApplied),
        error: '',
        stageText: result.success ? '已获取到可用定位，打卡已提交' : '已获取到可用定位，已完成业务校验',
        hasUsableLocation: true,
        sampleTimestamp: diagnostics.timestamp ?? null
      })
      state.checkInResult.success = Boolean(result.success)
      state.checkInResult.allowCheckIn = Boolean(result.allowCheckIn)
      state.checkInResult.action = result.action || ''
      state.checkInResult.status = result.status || ''
      state.checkInResult.distanceMeters = result.distanceMeters ?? null
      if (result.status === ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE) {
        state.checkInResult.reason = buildOutOfRangeReason(result.distanceMeters ?? diagnostics.localDistanceMeters, result.radiusMeters ?? diagnostics.radiusMeters, diagnostics.accuracyMeters)
      } else if (result.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID) {
        state.checkInResult.reason = buildInvalidLocationReason(diagnostics.accuracyMeters)
      } else if (result.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK) {
        state.checkInResult.reason = buildWeakLocationReason(diagnostics.accuracyMeters)
      } else {
        state.checkInResult.reason = result.failReason || result.reason || ''
      }
      await Promise.all([fetchList(), fetchCurrentLocation(), fetchLeadershipWorkspace()])
      if (result.success) {
        showToast(result.action === 'CHECK_OUT' ? CHECK_OUT_SUCCESS_MESSAGE : CHECK_IN_SUCCESS_MESSAGE)
      } else {
        showToast(state.checkInResult.reason || result.reason || CHECK_IN_FAILURE_MESSAGE)
      }
    } catch (error) {
      const environment = buildLocationEnvironmentDiagnostics()
      state.checkInResult.success = false
      state.checkInResult.allowCheckIn = false
      state.checkInResult.action = ''
      state.checkInResult.status = error?.code === 1 ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID : ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
      state.checkInResult.distanceMeters = null
      state.checkInResult.reason = buildInvalidLocationReason(null)
      console.warn('[attendance check-in location failure]', {
        errorCode: resolveBrowserLocationErrorCode(error?.code),
        locationSource: 'BROWSER_GEO',
        provider: 'BROWSER',
        stage: 'LEGACY_BROWSER_GEOLOCATION',
        latitude: null,
        longitude: null,
        accuracy: null,
        distanceMeters: null,
        radiusMeters: state.locationInfo.radiusMeters ?? null,
        fallbackEnabled: false,
        rawMessage: error?.message || String(error || ''),
        currentUrl: environment.url || '',
        userAgent: environment.userAgent || '',
        isWeChatEnv: isWechatBrowser()
      })
      updateCheckInVisualization({
        decisionBranch: error?.code === 1 ? 'INVALID' : 'LOCATION_ERROR',
        error: state.checkInResult.reason,
        stageText: '未获取到可用定位',
        hasUsableLocation: false
      })
      showToast(state.checkInResult.reason)
      return
    }
  } catch (error) {
    const message = resolveCheckInRequestErrorMessage(error)
    const inferredLocationSource = state.checkInVisualization.coordinateSource === 'wechat-gcj02'
      ? 'WECHAT_JSAPI'
      : (state.checkInVisualization.coordinateSource ? 'BROWSER_GEO' : '')
    const inferredProvider = inferredLocationSource === 'WECHAT_JSAPI'
      ? 'WECHAT'
      : (inferredLocationSource ? 'BROWSER' : '')
    const environment = buildLocationEnvironmentDiagnostics()
    if (error?.response) {
      await Promise.all([fetchList(), fetchCurrentLocation(), fetchLeadershipWorkspace()])
    }
    console.warn('[attendance check-in error]', {
      errorCode: error?.response?.data?.code || inferCheckInStatusFromErrorMessage(message) || 'API_EXCEPTION',
      locationSource: inferredLocationSource,
      provider: inferredProvider,
      stage: 'LEGACY_CHECK_IN_EXCEPTION',
      latitude: state.checkInVisualization.submitLatitude,
      longitude: state.checkInVisualization.submitLongitude,
      accuracy: state.checkInVisualization.accuracyMeters,
      distanceMeters: state.checkInVisualization.localDistanceMeters,
      radiusMeters: state.checkInVisualization.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
      fallbackEnabled: false,
      rawMessage: error?.response?.data?.message || error?.message || '',
      currentUrl: environment.url || '',
      userAgent: environment.userAgent || '',
      isWeChatEnv: isWechatBrowser(),
      status: error?.response?.status,
      data: error?.response?.data,
      message
    })
    state.checkInResult.success = false
    state.checkInResult.allowCheckIn = false
    state.checkInResult.action = ''
    state.checkInResult.status = inferCheckInStatusFromErrorMessage(message)
    state.checkInResult.distanceMeters = null
    state.checkInResult.reason = message
    updateCheckInVisualization({
      stageText: state.checkInVisualization.hasUsableLocation ? '已获取到可用定位，但提交失败' : state.checkInVisualization.stageText,
      error: message
    })
    if (!error?.response) {
      showToast(message || CHECK_IN_FAILURE_MESSAGE)
    }
  } finally {
    state.checkingIn = false
  }
}

async function handleCheckIn() {
  state.checkingIn = true
  resetCheckInVisualizationSubmission()
  try {
    updateCheckInVisualization({
      stageText: '正在获取定位...',
      hasUsableLocation: false,
      error: ''
    })

    const locationSelection = await resolveLocationSelectionOrFail()
    if (!locationSelection) {
      return
    }

    const diagnostics = locationSelection.diagnostics
    const usingWechatJsapi = locationSelection.source === 'WECHAT_JSAPI'

    updateCheckInVisualization({
      ...diagnostics,
      error: '',
      stageText: usingWechatJsapi ? '正在提交微信定位打卡...' : '正在提交打卡...',
      hasUsableLocation: true,
      sampleTimestamp: diagnostics?.timestamp ?? null
    })

    const environment = buildLocationEnvironmentDiagnostics()
    console.info('[attendance check-in submit diagnostics]', {
      source: locationSelection.source,
      provider: locationSelection.provider || '',
      errorCode: locationSelection.errorCode || '',
      stage: 'CHECK_IN_SUBMIT',
      selectedTimestamp: diagnostics?.timestamp,
      rawLatitude: diagnostics?.rawLatitude,
      rawLongitude: diagnostics?.rawLongitude,
      convertedLatitude: diagnostics?.convertedLatitude,
      convertedLongitude: diagnostics?.convertedLongitude,
      coordinateSource: diagnostics?.coordinateSource,
      converted: diagnostics?.coordinateSource === 'gcj02-adjusted',
      targetLatitude: diagnostics?.targetLatitude,
      targetLongitude: diagnostics?.targetLongitude,
      radiusMeters: diagnostics?.radiusMeters,
      accuracyMeters: diagnostics?.accuracyMeters,
      localDistanceMeters: diagnostics?.localDistanceMeters,
      overlap: diagnostics?.overlap,
      submitLatitude: diagnostics?.submitLatitude,
      submitLongitude: diagnostics?.submitLongitude,
      fallbackEnabled: false,
      currentUrl: environment.url || '',
      userAgent: environment.userAgent || '',
      isWeChatEnv: isWechatBrowser()
    })

    const response = await checkInApi(buildCheckInSubmissionPayload(locationSelection))
    const result = response.data || {}

    console.info('[attendance check-in response]', {
      ...diagnostics,
      responseDistanceMeters: result.distanceMeters ?? null,
      responseRadiusMeters: result.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
      responseStatus: result.status || '',
      responseReason: result.failReason || result.reason || ''
    })

    updateCheckInVisualization({
      ...diagnostics,
      localDistanceMeters: result.distanceMeters ?? diagnostics?.localDistanceMeters,
      decisionBranch: result.decisionBranch || '',
      weakToleranceApplied: Boolean(result.weakToleranceApplied),
      error: '',
      stageText: result.success
        ? (usingWechatJsapi ? '微信定位已提交，打卡已完成' : '已获取到可用定位，打卡已提交')
        : (usingWechatJsapi ? '微信定位已完成业务校验' : '已获取到可用定位，已完成业务校验'),
      hasUsableLocation: true,
      sampleTimestamp: diagnostics?.timestamp ?? null
    })

    state.checkInResult.success = Boolean(result.success)
    state.checkInResult.allowCheckIn = Boolean(result.allowCheckIn)
    state.checkInResult.action = result.action || ''
    state.checkInResult.status = result.status || ''
    state.checkInResult.distanceMeters = result.distanceMeters ?? null
    if (result.status === ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE) {
      state.checkInResult.reason = buildOutOfRangeReason(
        result.distanceMeters ?? diagnostics?.localDistanceMeters,
        result.radiusMeters ?? diagnostics?.radiusMeters,
        diagnostics?.accuracyMeters
      )
    } else if (result.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID) {
      state.checkInResult.reason = buildInvalidLocationReason(diagnostics?.accuracyMeters)
    } else if (result.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK) {
      state.checkInResult.reason = buildWeakLocationReason(diagnostics?.accuracyMeters)
    } else {
      state.checkInResult.reason = result.failReason || result.reason || ''
    }

    await Promise.all([fetchList(), fetchCurrentLocation(), fetchLeadershipWorkspace()])
    if (result.success) {
      showToast(result.action === 'CHECK_OUT' ? CHECK_OUT_SUCCESS_MESSAGE : CHECK_IN_SUCCESS_MESSAGE)
    } else {
      await reportLog(
        result.status === ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE
          || result.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
          || result.status === ATTENDANCE_CHECK_IN_STATUS.LOCATION_WEAK
          ? LOG_TYPES.FRONTEND_LOCATION_ERROR
          : LOG_TYPES.FRONTEND_API_ERROR,
        {
          traceId: response.traceId || '',
          module: 'ATTENDANCE',
          subModule: 'CHECK_IN',
          title: '打卡失败',
          summary: state.checkInResult.reason || result.reason || CHECK_IN_FAILURE_MESSAGE,
          diagnosis: buildLocationDiagnosticPayload({
            env: usingWechatJsapi ? 'WECHAT' : 'BROWSER',
            provider: locationSelection.source,
            stage: 'CHECK_IN_RESULT',
            errorCode: result.status || 'CHECK_IN_FAILED',
            rawMessage: result.failReason || result.reason || '',
            diagnostics,
            policy: locationAccuracyPolicy.value,
            extra: {
              result,
              locationSelectionSource: locationSelection.source
            }
          }).suggestion,
          errorCode: result.status || 'CHECK_IN_FAILED',
          message: result.failReason || result.reason || '',
          requestUrl: '/attendance/check-in',
          requestMethod: 'POST',
          requestParams: buildCheckInSubmissionPayload(locationSelection),
          responseStatus: 200,
          rawData: {
            result,
            diagnostics,
            visualization: state.checkInVisualization
          }
        }
      )
      showToast(state.checkInResult.reason || result.reason || CHECK_IN_FAILURE_MESSAGE)
    }
  } catch (error) {
    const message = resolveCheckInRequestErrorMessage(error)
    const inferredLocationSource = state.checkInVisualization.coordinateSource === 'wechat-gcj02'
      ? 'WECHAT_JSAPI'
      : (state.checkInVisualization.coordinateSource ? 'BROWSER_GEO' : '')
    const inferredProvider = inferredLocationSource === 'WECHAT_JSAPI'
      ? 'WECHAT'
      : (inferredLocationSource ? 'BROWSER' : '')
    const environment = buildLocationEnvironmentDiagnostics()
    if (error?.response) {
      await Promise.all([fetchList(), fetchCurrentLocation(), fetchLeadershipWorkspace()])
    }
    console.warn('[attendance check-in error]', {
      errorCode: error?.response?.data?.code || inferCheckInStatusFromErrorMessage(message) || 'API_EXCEPTION',
      locationSource: inferredLocationSource,
      provider: inferredProvider,
      stage: 'CHECK_IN_EXCEPTION',
      latitude: state.checkInVisualization.submitLatitude,
      longitude: state.checkInVisualization.submitLongitude,
      accuracy: state.checkInVisualization.accuracyMeters,
      distanceMeters: state.checkInVisualization.localDistanceMeters,
      radiusMeters: state.checkInVisualization.radiusMeters ?? state.locationInfo.radiusMeters ?? null,
      fallbackEnabled: false,
      rawMessage: error?.response?.data?.message || error?.message || '',
      currentUrl: environment.url || '',
      userAgent: environment.userAgent || '',
      isWeChatEnv: isWechatBrowser(),
      status: error?.response?.status,
      data: error?.response?.data,
      message
    })
    state.checkInResult.success = false
    state.checkInResult.allowCheckIn = false
    state.checkInResult.action = ''
    state.checkInResult.status = inferCheckInStatusFromErrorMessage(message)
    state.checkInResult.distanceMeters = null
    state.checkInResult.reason = message
    updateCheckInVisualization({
      stageText: state.checkInVisualization.hasUsableLocation ? '已获取到可用定位，但提交失败' : state.checkInVisualization.stageText,
      error: message
    })
    await reportLog(LOG_TYPES.FRONTEND_API_ERROR, {
      traceId: error?.response?.headers?.['x-trace-id'] || error?.response?.data?.traceId || '',
      module: 'ATTENDANCE',
      subModule: 'CHECK_IN',
      title: '打卡接口异常',
      summary: message || CHECK_IN_FAILURE_MESSAGE,
      diagnosis: '打卡请求在提交阶段失败，请结合 traceId、接口响应和当前定位快照继续排查。',
      errorCode: error?.response?.data?.code || inferCheckInStatusFromErrorMessage(message) || 'CHECK_IN_API_EXCEPTION',
      message,
      requestUrl: '/attendance/check-in',
      requestMethod: 'POST',
      requestParams: {
        latitude: state.checkInVisualization.submitLatitude,
        longitude: state.checkInVisualization.submitLongitude,
        accuracyMeters: state.checkInVisualization.accuracyMeters
      },
      responseStatus: error?.response?.status ?? null,
      rawData: {
        responseData: error?.response?.data || null,
        visualization: state.checkInVisualization,
        stack: error?.stack || ''
      }
    })
    if (!error?.response) {
      showToast(message || CHECK_IN_FAILURE_MESSAGE)
    }
  } finally {
    state.checkingIn = false
  }
}

function openEditForm(item) {
  state.form = {
    id: item.id,
    userId: item.userId ? String(item.userId) : '',
    selectedUser: normalizeFormSelectedUser({
      id: item.userId,
      realName: item.realName,
      username: item.username,
      mobile: item.mobile,
      unitName: item.unitName
    }),
    attendanceDate: item.attendanceDate || toInputDate(new Date()),
    checkInTime: item.checkInTime ? toInputDateTime(item.checkInTime) : '',
    checkOutTime: item.checkOutTime ? toInputDateTime(item.checkOutTime) : '',
    checkInAddress: item.checkInAddress || '',
    checkOutAddress: item.checkOutAddress || '',
    validFlag: Number(item.validFlag) || 0
  }
  state.userPicker.loading = false
  state.userPicker.searched = false
  state.userPicker.keyword = item.realName || item.mobile || item.username || ''
  state.userPicker.candidates = []
}

async function handleSave() {
  if (!state.form.attendanceDate) {
    showToast('请选择考勤日期')
    return
  }

  state.saving = true
  try {
    await saveAttendanceApi({
      id: state.form.id || undefined,
      userId: state.form.userId ? Number(state.form.userId) : Number(userStore.userInfo?.userId || 0),
      attendanceDate: state.form.attendanceDate,
      checkInTime: state.form.checkInTime ? toBackendDateTime(state.form.checkInTime) : undefined,
      checkOutTime: state.form.checkOutTime ? toBackendDateTime(state.form.checkOutTime) : undefined,
      checkInAddress: state.form.checkInAddress || undefined,
      checkOutAddress: state.form.checkOutAddress || undefined,
      validFlag: Number(state.form.validFlag)
    })
    showToast(state.form.id ? '考勤修改成功' : '考勤补录成功')
    resetForm()
    await fetchList()
  } catch (error) {
    showToast(error.message || '考勤保存失败')
  } finally {
    state.saving = false
  }
}

async function handleDelete(item) {
  state.deletingId = item.id
  try {
    await showConfirmDialog({
      title: '确认删除',
      message: `确认删除 ${item.realName || item.username} 在 ${item.attendanceDate} 的考勤吗？`
    })
    await deleteAttendanceApi(item.id)
    showToast('考勤删除成功')
    if (state.form.id === item.id) {
      resetForm()
    }
    await fetchList()
  } catch (error) {
    if (error?.message && error.message !== 'cancel') {
      showToast(error.message || '考勤删除失败')
    }
  } finally {
    state.deletingId = null
  }
}

function escapeCsvValue(value) {
  const text = value == null ? '' : String(value)
  if (text.includes(',') || text.includes('"') || text.includes('\n')) {
    return `"${text.replace(/"/g, '""')}"`
  }
  return text
}

function sanitizeFileSegment(value) {
  const text = value == null ? '' : String(value).trim()
  if (!text) {
    return 'all'
  }
  return text.replace(/[^\w\u4e00-\u9fa5-]+/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '') || 'all'
}

function buildRangeFileSegment(dateFrom, dateTo) {
  if (dateFrom && dateTo) {
    return `${dateFrom}_${dateTo}`
  }
  if (dateFrom) {
    return `${dateFrom}_start`
  }
  if (dateTo) {
    return `until_${dateTo}`
  }
  return 'all-time'
}

async function handleExport() {
  if (!state.list.length) {
    showToast('当前没有可导出的考勤记录')
    return
  }
  if (!validateQueryDateRange()) {
    return
  }
  state.exporting = true
  try {
    const headers = [
      '记录ID',
      '用户ID',
      '账号',
      '姓名',
      '组织',
      '考勤日期',
      '上班时间',
      '下班时间',
      '打卡状态值',
      '打卡状态文案',
      '距离(米)',
      '失败原因',
      '纬度',
      '经度',
      '上班地点',
      '下班地点'
    ]
    const metaRows = [
      ['查询范围', listRangeText.value],
      ['状态筛选', state.queryForm.checkInStatus ? getAttendanceStatusLabel(state.queryForm.checkInStatus) : '全部状态'],
      ['筛选摘要', listFilterSummary.value || '无'],
      ['联动摘要', listLinkageSummary.value || '无']
    ]
    const rows = state.list.map((item) => [
      item.id,
      item.userId,
      item.username || '',
      item.realName || '',
      item.unitName || '',
      item.attendanceDate || '',
      formatDateTime(item.checkInTime),
      formatDateTime(item.checkOutTime),
      item.checkInResult || '',
      resultLabel(item.checkInResult),
      item.checkInDistanceMeters ?? '',
      item.checkInFailReason || '',
      item.checkInLatitude ?? '',
      item.checkInLongitude ?? '',
      item.checkInAddress || '',
      item.checkOutAddress || ''
    ])
    const csvContent = [...metaRows, [], headers, ...rows].map((row) => row.map(escapeCsvValue).join(',')).join('\n')
    const blob = new Blob([`\ufeff${csvContent}`], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `attendance-records-${sanitizeFileSegment(selectedAbnormalUserName.value)}-${sanitizeFileSegment(state.queryForm.checkInStatus || 'all')}-${buildRangeFileSegment(state.queryForm.dateFrom || undefined, state.queryForm.dateTo || undefined)}-${toInputDate(new Date())}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('考勤记录导出成功')
  } catch (error) {
    showToast(error.message || '考勤记录导出失败')
  } finally {
    state.exporting = false
  }
}

async function handleExportAbnormalRanks() {
  if (!state.abnormalMonitor.topUsers.length) {
    showToast('当前没有可导出的异常榜单')
    return
  }
  if (!validateQueryDateRange()) {
    return
  }
  state.abnormalExporting = true
  try {
    const abnormalMonitorPayload = buildAbnormalMonitorPayload()
    const headers = ['排名', '用户ID', '账号', '姓名', '组织', '异常次数', '总记录数', '异常率', '风险分', '风险等级', '近7天趋势', '主要异常原因', '原因标签', '预警']
    const rows = state.abnormalMonitor.topUsers.map((item, index) => [
      index + 1,
      item.userId,
      item.username || '',
      item.realName || '',
      item.unitName || '',
      item.abnormalCount ?? 0,
      item.totalCount ?? 0,
      formatPercent(item.abnormalRate),
      item.riskScore ?? 0,
      riskLevelLabel(item.riskLevel),
      trendDirectionLabel(item.trendDirection),
      item.mainReasonLabel || '',
      item.mainReasonTag || '',
      item.alertTriggered ? item.alertRuleText || '是' : '否'
    ])
    const metaRows = [
      ['统计范围', abnormalMonitorRangeText.value],
      ['异常口径', 'check_in_result 不属于 CHECK_IN_SUCCESS / CHECK_OUT_SUCCESS'],
      ['当前选中异常用户', state.abnormalSelection.activeUserId ? selectedAbnormalUserName.value : '未指定']
    ]
    const csvContent = [...metaRows, [], headers, ...rows].map((row) => row.map(escapeCsvValue).join(',')).join('\n')
    const blob = new Blob([`\ufeff${csvContent}`], { type: 'text/csv;charset=utf-8;' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `attendance-abnormal-top10-${sanitizeFileSegment(selectedAbnormalUserName.value)}-${buildRangeFileSegment(abnormalMonitorPayload.dateFrom, abnormalMonitorPayload.dateTo)}-${toInputDate(new Date())}.csv`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    window.URL.revokeObjectURL(url)
    showToast('异常榜单导出成功')
  } catch (error) {
    showToast(error.message || '异常榜单导出失败')
  } finally {
    state.abnormalExporting = false
  }
}

onMounted(() => {
  fetchCurrentLocation()
  fetchList()
  fetchLeadershipWorkspace()
  nextTick(() => {
    syncCheckInMap()
  })
})

watch(
  () => [
    state.locationInfo.latitude,
    state.locationInfo.longitude,
    state.locationInfo.radiusMeters,
    state.checkInVisualization.submitLatitude,
    state.checkInVisualization.submitLongitude,
    state.checkInVisualization.accuracyMeters
  ],
  () => {
    syncCheckInMap()
  },
  { flush: 'post' }
)

watch(leadershipFilteredMembers, (members) => {
  if (!hasSubordinates.value) {
    state.leadershipSelectedUserId = null
    state.leadershipRecentAbnormalRecords = []
    state.leadershipDetailLoading = false
    return
  }
  if (!members.length) {
    state.leadershipSelectedUserId = null
    state.leadershipRecentAbnormalRecords = []
    state.leadershipDetailLoading = false
    return
  }
  const hasSelectedMember = members.some(item => item.userId === state.leadershipSelectedUserId)
  if (!hasSelectedMember) {
    void handleSelectLeadershipMember(members[0])
  }
})

onBeforeUnmount(() => {
  if (checkInMapInstance) {
    checkInMapInstance.destroy()
    checkInMapInstance = null
    checkInAmap = null
    checkInMapElements = null
  }
})
</script>

<style scoped>
.attendance-leadership-section {
  display: grid;
  gap: 20px;
  margin-bottom: 20px;
}

.attendance-leadership-workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.02fr) minmax(380px, 0.92fr);
  gap: 22px;
  align-items: start;
}

.attendance-leadership-main {
  min-width: 0;
}

.attendance-leadership-aside {
  position: sticky;
  top: 24px;
}

.attendance-leadership-detail-mobile {
  display: none;
}

.attendance-legacy-tools {
  margin-bottom: 18px;
  border: 1px solid rgba(148, 163, 184, 0.22);
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.94);
  overflow: hidden;
}

.attendance-legacy-tools__summary {
  list-style: none;
  cursor: pointer;
  padding: 16px 18px;
  background: linear-gradient(180deg, rgba(248, 250, 252, 0.96), rgba(241, 245, 249, 0.96));
}

.attendance-legacy-tools__summary::-webkit-details-marker {
  display: none;
}

.attendance-legacy-tools__summary strong {
  display: block;
  font-size: 15px;
  color: #0f172a;
}

.attendance-legacy-tools__summary span {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.6;
  color: #64748b;
}

.attendance-legacy-tools > .panel:first-of-type {
  margin-top: 0;
}

.action-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.query-grid {
  display: grid;
  gap: 8px;
}

.quick-range-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 8px;
  margin-top: 10px;
}

.quick-range-label {
  font-size: 13px;
  color: #4b5563;
}

.quick-range-button {
  border: 1px solid #dbe3ee;
  background: #f8fafc;
  color: #334155;
  border-radius: 999px;
  padding: 6px 12px;
  font-size: 13px;
  line-height: 1.2;
}

.quick-range-button-active {
  border-color: #1989fa;
  background: #e8f3ff;
  color: #1989fa;
}

.panel {
  margin-bottom: 16px;
  padding: 12px;
  background: #fff;
  border-radius: 12px;
}

.panel-title {
  margin-bottom: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
}

.panel-hint,
.attendance-meta {
  font-size: 13px;
  color: #4b5563;
  line-height: 1.6;
}

.location-card {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.query-context-card {
  margin-top: 12px;
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.panel-title-sub {
  margin-top: 12px;
}

.attendance-map {
  width: 100%;
  height: 260px;
  margin-top: 10px;
  border-radius: 12px;
  overflow: hidden;
  border: 1px solid #dbe3ee;
  background: #eef4fb;
}

.diagnostic-grid {
  display: grid;
  gap: 4px;
  margin-top: 10px;
}

.selected-user-card {
  margin-top: 10px;
  padding: 12px;
  border-radius: 12px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
}

.attendance-guide {
  margin-top: 10px;
  color: #b45309;
  font-size: 13px;
  line-height: 1.6;
}

.panel-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 10px;
}

.select-field {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 10px 0;
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
}

.list-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.user-candidate-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 10px;
}

.user-candidate-item {
  width: 100%;
  text-align: left;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  padding: 10px 12px;
  background: #fff;
}

.user-candidate-item-active {
  border-color: #1989fa;
  background: #eff6ff;
}

.user-candidate-title {
  margin-bottom: 4px;
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(140px, 1fr));
  gap: 12px;
}

.trend-overview-grid {
  margin-top: 4px;
}

.rank-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.rank-item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 12px;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  background: #fff;
  text-align: left;
}

.rank-item-active {
  background: #fff1f2;
  border-color: #fb7185;
  box-shadow: inset 0 0 0 1px #fb7185;
}

.rank-order {
  width: 36px;
  height: 36px;
  border-radius: 999px;
  background: #0f172a;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.rank-body {
  flex: 1;
  min-width: 0;
}

.rank-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2937;
  line-height: 1.5;
}

.rank-stats {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 6px;
}

.rank-badge {
  padding: 6px 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #334155;
  font-size: 12px;
}

.rank-badge-danger {
  background: #fff1f2;
  border-color: #fecdd3;
  color: #be123c;
}

.rank-badge-primary {
  background: #dbeafe;
  border-color: #bfdbfe;
  color: #1d4ed8;
}

.rank-badge-warning {
  background: #fef3c7;
  border-color: #fde68a;
  color: #b45309;
}

.summary-card {
  padding: 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.user-summary-card {
  margin-bottom: 4px;
}

.summary-card-danger {
  background: #fff1f2;
  border-color: #fecdd3;
}

.summary-label {
  font-size: 13px;
  color: #64748b;
}

.summary-value {
  margin-top: 8px;
  font-size: 24px;
  font-weight: 700;
  color: #0f172a;
}

.summary-value-success {
  color: #15803d;
}

.summary-value-danger {
  color: #be123c;
}

.status-summary-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.status-summary-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 999px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.reason-distribution-list {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.reason-distribution-wrap {
  margin-top: 12px;
}

.reason-distribution-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
}

.trend-wrap {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.trend-header {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.trend-chart {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(96px, 1fr));
  gap: 12px;
  align-items: end;
}

.trend-bar-col {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 10px 8px;
  border-radius: 12px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  text-align: center;
}

.trend-bar-col-active {
  background: #fff1f2;
  border-color: #fb7185;
  box-shadow: inset 0 0 0 1px #fb7185;
}

.trend-bar-stack {
  position: relative;
  width: 36px;
  height: 140px;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 4px;
}

.trend-bar {
  width: 100%;
  border-radius: 999px 999px 8px 8px;
}

.trend-bar-total {
  background: #cbd5e1;
}

.trend-bar-abnormal {
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  background: #e11d48;
}

.trend-date {
  font-size: 12px;
  color: #0f172a;
  font-weight: 600;
}

.trend-meta {
  font-size: 12px;
  color: #64748b;
  text-align: center;
  line-height: 1.5;
}

.attendance-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.attendance-tag-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.state-block {
  padding: 20px 0;
}

@media (max-width: 960px) {
  .attendance-leadership-workspace {
    grid-template-columns: 1fr;
  }

  .attendance-leadership-aside {
    display: none;
  }

  .attendance-leadership-detail-mobile {
    display: block;
  }

  .attendance-legacy-tools__summary {
    padding: 14px 16px;
  }
}
</style>
