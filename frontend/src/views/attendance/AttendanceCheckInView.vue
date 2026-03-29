<template>
  <AppPageShell title="考勤管理" description="当前页已接入最小可用考勤工作流：列表、查询、打卡、补录编辑和删除。">
    <template #title-extra>
      <PageHelp page-key="attendance" />
    </template>
    <template #actions>
      <div class="action-row" data-guide="attendance-checkin">
        <van-button type="primary" :loading="state.checkingIn" :disabled="pageBusy" @click="handleCheckIn">立即打卡</van-button>
        <van-button plain type="success" :loading="state.exporting" :disabled="pageBusy || !state.list.length" @click="handleExport">导出记录</van-button>
        <van-button plain type="primary" :loading="state.loading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
      </div>
    </template>

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
          <div class="panel-hint">可直接点击“立即打卡”，或在上方表单补录一条考勤。</div>
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
  </AppPageShell>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import PageHelp from '@/components/PageHelp.vue'
import { queryUserPageApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { loadAmapSdk } from '@/utils/amap'
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
const INVALID_ACCURACY_METERS = 1000
const WEAK_ACCURACY_FLOOR_METERS = 80
const CHECK_IN_MAP_DEFAULT_ZOOM = 14
const CHECK_IN_MAP_COLORS = {
  target: '#dc2626',
  current: '#2563eb',
  line: '#0f766e'
}

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

function roundCoordinate(value) {
  if (value == null || Number.isNaN(Number(value))) {
    return null
  }
  return Number(Number(value).toFixed(6))
}

function calculateDistanceMeters(latitude1, longitude1, latitude2, longitude2) {
  const values = [latitude1, longitude1, latitude2, longitude2].map(item => Number(item))
  if (values.some(item => Number.isNaN(item))) {
    return null
  }
  const [lat1, lng1, lat2, lng2] = values
  const earthRadiusMeters = 6371000
  const latDistance = ((lat2 - lat1) * Math.PI) / 180
  const lngDistance = ((lng2 - lng1) * Math.PI) / 180
  const sinLat = Math.sin(latDistance / 2)
  const sinLng = Math.sin(lngDistance / 2)
  const a = sinLat * sinLat + Math.cos((lat1 * Math.PI) / 180) * Math.cos((lat2 * Math.PI) / 180) * sinLng * sinLng
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return Math.round(earthRadiusMeters * c)
}

function outOfChina(longitude, latitude) {
  return longitude < 72.004 || longitude > 137.8347 || latitude < 0.8293 || latitude > 55.8271
}

function transformLatitude(longitude, latitude) {
  let result = -100 + 2 * longitude + 3 * latitude + 0.2 * latitude * latitude + 0.1 * longitude * latitude + 0.2 * Math.sqrt(Math.abs(longitude))
  result += ((20 * Math.sin(6 * longitude * Math.PI) + 20 * Math.sin(2 * longitude * Math.PI)) * 2) / 3
  result += ((20 * Math.sin(latitude * Math.PI) + 40 * Math.sin((latitude / 3) * Math.PI)) * 2) / 3
  result += ((160 * Math.sin((latitude / 12) * Math.PI) + 320 * Math.sin((latitude * Math.PI) / 30)) * 2) / 3
  return result
}

function transformLongitude(longitude, latitude) {
  let result = 300 + longitude + 2 * latitude + 0.1 * longitude * longitude + 0.1 * longitude * latitude + 0.1 * Math.sqrt(Math.abs(longitude))
  result += ((20 * Math.sin(6 * longitude * Math.PI) + 20 * Math.sin(2 * longitude * Math.PI)) * 2) / 3
  result += ((20 * Math.sin(longitude * Math.PI) + 40 * Math.sin((longitude / 3) * Math.PI)) * 2) / 3
  result += ((150 * Math.sin((longitude / 12) * Math.PI) + 300 * Math.sin((longitude / 30) * Math.PI)) * 2) / 3
  return result
}

function wgs84ToGcj02(longitude, latitude) {
  const lng = Number(longitude)
  const lat = Number(latitude)
  if (Number.isNaN(lng) || Number.isNaN(lat) || outOfChina(lng, lat)) {
    return { longitude: lng, latitude: lat }
  }
  const a = 6378245.0
  const ee = 0.00669342162296594323
  let dLat = transformLatitude(lng - 105.0, lat - 35.0)
  let dLng = transformLongitude(lng - 105.0, lat - 35.0)
  const radLat = (lat / 180.0) * Math.PI
  let magic = Math.sin(radLat)
  magic = 1 - ee * magic * magic
  const sqrtMagic = Math.sqrt(magic)
  dLat = (dLat * 180.0) / (((a * (1 - ee)) / (magic * sqrtMagic)) * Math.PI)
  dLng = (dLng * 180.0) / ((a / sqrtMagic) * Math.cos(radLat) * Math.PI)
  return {
    longitude: lng + dLng,
    latitude: lat + dLat
  }
}

function buildOutOfRangeReason(distanceMeters, radiusMeters, accuracyMeters) {
  if (distanceMeters == null || radiusMeters == null) {
    return '超出单位打卡范围'
  }
  let reason = `当前位置距离打卡点 ${distanceMeters} 米，允许半径 ${radiusMeters} 米`
  if (accuracyMeters != null && accuracyMeters > radiusMeters) {
    reason += `；当前定位精度约 ${accuracyMeters} 米，请尽量在室外空旷区域重试`
  }
  return reason
}

function buildInvalidLocationReason(accuracyMeters) {
  if (accuracyMeters != null) {
    return `未获取到可用定位，当前定位精度约 ${accuracyMeters} 米，请检查定位权限或定位服务后重试`
  }
  return '未获取到可用定位，请检查定位权限或定位服务后重试'
}

function buildWeakLocationReason(accuracyMeters) {
  if (accuracyMeters != null) {
    return `当前定位质量较差，定位精度约 ${accuracyMeters} 米，室内信号可能较弱，建议稍等定位稳定或靠窗后重试`
  }
  return '当前定位质量较差，室内信号可能较弱，建议稍等定位稳定或靠窗后重试'
}

function isInvalidGeolocationSample(position) {
  const latitude = Number(position?.coords?.latitude)
  const longitude = Number(position?.coords?.longitude)
  const accuracyMeters = position?.coords?.accuracy == null ? null : Math.round(Number(position.coords.accuracy))
  if (Number.isNaN(latitude) || Number.isNaN(longitude)) {
    return true
  }
  if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
    return true
  }
  if (Math.abs(latitude) < 0.000001 && Math.abs(longitude) < 0.000001) {
    return true
  }
  return accuracyMeters != null && accuracyMeters > INVALID_ACCURACY_METERS
}

function isWeakAccuracy(accuracyMeters, radiusMeters) {
  if (accuracyMeters == null) {
    return false
  }
  return accuracyMeters > Math.max(Number(radiusMeters) || 0, WEAK_ACCURACY_FLOOR_METERS)
}

function buildCheckInDiagnostics(position) {
  const rawLatitude = Number(position?.coords?.latitude)
  const rawLongitude = Number(position?.coords?.longitude)
  const accuracyMeters = position?.coords?.accuracy == null ? null : Math.round(Number(position.coords.accuracy))
  const timestamp = Number(position?.timestamp || Date.now())
  const targetLatitude = Number(state.locationInfo.latitude)
  const targetLongitude = Number(state.locationInfo.longitude)
  const radiusMeters = state.locationInfo.radiusMeters == null ? null : Number(state.locationInfo.radiusMeters)
  const rawDistanceMeters = calculateDistanceMeters(rawLatitude, rawLongitude, targetLatitude, targetLongitude)
  const converted = wgs84ToGcj02(rawLongitude, rawLatitude)
  const convertedDistanceMeters = calculateDistanceMeters(converted.latitude, converted.longitude, targetLatitude, targetLongitude)
  const useConverted = rawDistanceMeters != null
    && convertedDistanceMeters != null
    && convertedDistanceMeters < rawDistanceMeters
  const submitLatitude = roundCoordinate(useConverted ? converted.latitude : rawLatitude)
  const submitLongitude = roundCoordinate(useConverted ? converted.longitude : rawLongitude)
  const localDistanceMeters = calculateDistanceMeters(submitLatitude, submitLongitude, targetLatitude, targetLongitude)
  const overlap = localDistanceMeters != null
    && radiusMeters != null
    && accuracyMeters != null
    && localDistanceMeters <= radiusMeters + accuracyMeters
  return {
    rawLatitude: roundCoordinate(rawLatitude),
    rawLongitude: roundCoordinate(rawLongitude),
    convertedLatitude: roundCoordinate(converted.latitude),
    convertedLongitude: roundCoordinate(converted.longitude),
    submitLatitude,
    submitLongitude,
    targetLatitude: roundCoordinate(targetLatitude),
    targetLongitude: roundCoordinate(targetLongitude),
    radiusMeters,
    accuracyMeters,
    timestamp,
    rawDistanceMeters,
    convertedDistanceMeters,
    localDistanceMeters,
    overlap,
    coordinateSource: useConverted ? 'gcj02-adjusted' : 'browser-raw'
  }
}

function compareCheckInDiagnostics(current, next) {
  const currentWeak = isWeakAccuracy(current.accuracyMeters, current.radiusMeters)
  const nextWeak = isWeakAccuracy(next.accuracyMeters, next.radiusMeters)
  if (currentWeak !== nextWeak) {
    return currentWeak ? 1 : -1
  }
  const currentAccuracy = current.accuracyMeters ?? Number.POSITIVE_INFINITY
  const nextAccuracy = next.accuracyMeters ?? Number.POSITIVE_INFINITY
  if (currentAccuracy !== nextAccuracy) {
    return currentAccuracy - nextAccuracy
  }
  const currentTimestamp = current.timestamp ?? 0
  const nextTimestamp = next.timestamp ?? 0
  if (currentTimestamp !== nextTimestamp) {
    return nextTimestamp - currentTimestamp
  }
  const currentDistance = current.localDistanceMeters ?? Number.POSITIVE_INFINITY
  const nextDistance = next.localDistanceMeters ?? Number.POSITIVE_INFINITY
  return currentDistance - nextDistance
}

function isLocalhostHost(hostname) {
  return hostname === 'localhost' || hostname === '127.0.0.1' || hostname === '::1'
}

function isPrivateIpv4Host(hostname) {
  return /^10\./.test(hostname)
    || /^192\.168\./.test(hostname)
    || /^172\.(1[6-9]|2\d|3[0-1])\./.test(hostname)
}

function buildLocationEnvironmentDiagnostics() {
  const { protocol, hostname, href } = window.location
  return {
    userAgent: navigator.userAgent,
    url: href,
    protocol,
    hostname,
    isHttps: protocol === 'https:',
    isLocalhost: isLocalhostHost(hostname),
    isIntranet: isLocalhostHost(hostname) || isPrivateIpv4Host(hostname) || (!hostname.includes('.') && hostname !== '')
  }
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

function getCurrentPositionOnce() {
  return new Promise((resolve, reject) => {
    navigator.geolocation.getCurrentPosition(resolve, reject, {
      enableHighAccuracy: true,
      timeout: GEOLOCATION_TIMEOUT_MS,
      maximumAge: 0
    })
  })
}

async function collectBestGeolocationDiagnostics() {
  const samples = []
  let lastError = null
  console.info('[attendance geolocation environment]', buildLocationEnvironmentDiagnostics())
  updateCheckInVisualization({
    stageText: '正在获取定位...',
    hasUsableLocation: false,
    error: ''
  })
  for (let index = 0; index < GEOLOCATION_SAMPLE_COUNT; index += 1) {
    try {
      const position = await getCurrentPositionOnce()
      const diagnostics = buildCheckInDiagnostics(position)
      diagnostics.sampleIndex = index + 1
      diagnostics.invalidSample = isInvalidGeolocationSample(position)
      diagnostics.weakSample = isWeakAccuracy(diagnostics.accuracyMeters, diagnostics.radiusMeters)
      samples.push(diagnostics)
      console.info('[attendance geolocation sample]', {
        sampleIndex: diagnostics.sampleIndex,
        timestamp: diagnostics.timestamp,
        latitude: diagnostics.rawLatitude,
        longitude: diagnostics.rawLongitude,
        accuracyMeters: diagnostics.accuracyMeters,
        convertedLatitude: diagnostics.convertedLatitude,
        convertedLongitude: diagnostics.convertedLongitude,
        converted: diagnostics.coordinateSource === 'gcj02-adjusted',
        localDistanceMeters: diagnostics.localDistanceMeters,
        invalidSample: diagnostics.invalidSample,
        weakSample: diagnostics.weakSample
      })
    } catch (error) {
      lastError = error
      console.warn('[attendance geolocation sample error]', {
        sampleIndex: index + 1,
        code: error?.code,
        message: error?.message || ''
      })
      if (error?.code === 1) {
        throw error
      }
    }
  }
  updateCheckInVisualization({
    stageText: '正在校验定位质量...'
  })
  const validSamples = samples.filter(item => !item.invalidSample)
  if (!validSamples.length) {
    return {
      diagnostics: null,
      samples,
      invalidReason: buildInvalidLocationReason(samples[0]?.accuracyMeters ?? null),
      lastError
    }
  }
  validSamples.sort(compareCheckInDiagnostics)
  console.info('[attendance geolocation selected sample]', {
    selected: validSamples[0],
    sampleCount: samples.length
  })
  return {
    diagnostics: validSamples[0],
    samples,
    invalidReason: '',
    lastError
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
  if (backendMessage && String(backendMessage).trim()) {
    return String(backendMessage).trim()
  }
  if (error?.message && String(error.message).trim()) {
    return String(error.message).trim()
  }
  return '打卡失败，请稍后重试'
}

function inferCheckInStatusFromErrorMessage(message) {
  if (!message) {
    return ''
  }
  if (message.includes('定位无效')) {
    return ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
  }
  if (message.includes('定位质量较差') || message.includes('室内信号')) {
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
  if (message.includes('超出单位打卡范围')) {
    return ATTENDANCE_CHECK_IN_STATUS.OUT_OF_RANGE
  }
  if (message.includes('今日考勤已完成')) {
    return ATTENDANCE_CHECK_IN_STATUS.ALREADY_FINISHED
  }
  return ''
}

async function handleCheckIn() {
  if (!navigator.geolocation) {
    state.checkInResult.success = false
    state.checkInResult.allowCheckIn = false
    state.checkInResult.action = ''
    state.checkInResult.status = ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
    state.checkInResult.distanceMeters = null
    state.checkInResult.reason = '当前环境不支持定位'
    updateCheckInVisualization({
      stageText: '未获取到可用定位',
      hasUsableLocation: false,
      error: '当前环境不支持定位'
    })
    showToast('当前环境不支持定位')
    return
  }
  state.checkingIn = true
  resetCheckInVisualizationSubmission()
  try {
    try {
      const collected = await collectBestGeolocationDiagnostics()
      if (!collected.diagnostics) {
        updateCheckInVisualization({
          ...(collected.samples[collected.samples.length - 1] || {}),
          error: collected.invalidReason || '未获取到可用定位，请检查定位服务后重试',
          decisionBranch: 'INVALID',
          overlap: false,
          weakToleranceApplied: false,
          stageText: '未获取到可用定位',
          hasUsableLocation: false,
          sampleTimestamp: collected.samples[collected.samples.length - 1]?.timestamp ?? null
        })
        state.checkInResult.success = false
        state.checkInResult.allowCheckIn = false
        state.checkInResult.action = ''
        state.checkInResult.status = ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID
        state.checkInResult.distanceMeters = null
        state.checkInResult.reason = collected.invalidReason || '未获取到可用定位，请检查定位服务后重试'
        showToast(state.checkInResult.reason)
        return
      }
      const diagnostics = collected.diagnostics
      updateCheckInVisualization({
        ...diagnostics,
        error: '',
        stageText: '正在提交打卡...',
        hasUsableLocation: true,
        sampleTimestamp: diagnostics.timestamp ?? null
      })
      console.info('[attendance check-in submit diagnostics]', {
        environment: buildLocationEnvironmentDiagnostics(),
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
        submitLongitude: diagnostics.submitLongitude
      })
      const response = await checkInApi({
        address: `浏览器定位 ${diagnostics.submitLatitude}, ${diagnostics.submitLongitude}`,
        latitude: diagnostics.submitLatitude,
        longitude: diagnostics.submitLongitude,
        accuracyMeters: diagnostics.accuracyMeters
      })
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
      await Promise.all([fetchList(), fetchCurrentLocation()])
      if (result.success) {
        showToast(result.action === 'CHECK_OUT' ? '下班时间已补齐' : '上班打卡成功')
      } else {
        showToast(state.checkInResult.reason || result.reason || '打卡失败')
      }
    } catch (error) {
      state.checkInResult.success = false
      state.checkInResult.allowCheckIn = false
      state.checkInResult.action = ''
      state.checkInResult.status = error?.code === 1 ? ATTENDANCE_CHECK_IN_STATUS.LOCATION_INVALID : ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
      state.checkInResult.distanceMeters = null
      state.checkInResult.reason = error?.code === 1 ? '定位权限被拒绝，请检查浏览器定位权限' : '定位失败，请检查定位服务后重试'
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
    if (error?.response) {
      await Promise.all([fetchList(), fetchCurrentLocation()])
    }
    console.warn('[attendance check-in error]', {
      message,
      status: error?.response?.status,
      data: error?.response?.data
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
      showToast(message)
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
</style>
