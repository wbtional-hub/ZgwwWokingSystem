<template>
  <AppPageShell title="考勤管理" description="当前页已接入最小可用考勤工作流：列表、查询、打卡、补录编辑和删除。">
    <template #actions>
      <div class="action-row">
        <van-button type="primary" :loading="state.checkingIn" :disabled="pageBusy" @click="handleCheckIn">立即打卡</van-button>
        <van-button plain type="success" :loading="state.exporting" :disabled="pageBusy || !state.list.length" @click="handleExport">导出记录</van-button>
        <van-button plain type="primary" :loading="state.loading" :disabled="pageBusy" @click="fetchList">刷新列表</van-button>
      </div>
    </template>

    <section class="panel">
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

    <section class="panel">
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
        <div class="attendance-meta">距离：{{ state.checkInResult.distanceMeters == null ? '-' : `${state.checkInResult.distanceMeters}米` }}</div>
        <div class="attendance-meta">允许打卡：{{ state.checkInResult.allowCheckIn === null ? '-' : state.checkInResult.allowCheckIn ? '是' : '否' }}</div>
        <div class="attendance-meta">原因：{{ state.checkInResult.reason || '-' }}</div>
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
          <div v-if="state.abnormalSelection.activeUserId || state.trendDateLinkage.selectedDate" class="panel-actions">
            <van-button size="small" plain type="primary" :disabled="pageBusy" @click="handleClearAbnormalSelection">
              恢复异常联动前筛选
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
import { computed, onMounted, reactive } from 'vue'
import { showConfirmDialog, showToast } from 'vant'
import AppPageShell from '@/components/layout/AppPageShell.vue'
import { queryUserPageApi } from '@/api/user'
import { useUserStore } from '@/stores/user'
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
    state.locationInfo.radiusMeters = data.radiusMeters ?? null
    state.locationInfo.allowCheckIn = Boolean(data.allowCheckIn)
    state.locationInfo.status = data.status || ''
    state.locationInfo.reason = data.reason || ''
  } catch (error) {
    state.locationInfo.unitName = ''
    state.locationInfo.locationName = ''
    state.locationInfo.address = ''
    state.locationInfo.radiusMeters = null
    state.locationInfo.allowCheckIn = false
    state.locationInfo.status = ''
    state.locationInfo.reason = error.message || '打卡点信息加载失败'
  } finally {
    state.locationLoading = false
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
    showToast('当前环境不支持定位')
    return
  }
  state.checkingIn = true
  try {
    let position
    try {
      position = await new Promise((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, { enableHighAccuracy: true, timeout: 10000 })
      })
    } catch (error) {
      state.checkInResult.success = false
      state.checkInResult.allowCheckIn = false
      state.checkInResult.action = ''
      state.checkInResult.status = ATTENDANCE_CHECK_IN_STATUS.LOCATION_REQUIRED
      state.checkInResult.distanceMeters = null
      state.checkInResult.reason = error?.code === 1 ? '定位权限被拒绝' : '定位失败，请重试'
      showToast(state.checkInResult.reason)
      return
    }
    const response = await checkInApi({
      address: `浏览器定位 ${position.coords.latitude.toFixed(6)}, ${position.coords.longitude.toFixed(6)}`,
      latitude: Number(position.coords.latitude.toFixed(6)),
      longitude: Number(position.coords.longitude.toFixed(6))
    })
    const result = response.data || {}
    state.checkInResult.success = Boolean(result.success)
    state.checkInResult.allowCheckIn = Boolean(result.allowCheckIn)
    state.checkInResult.action = result.action || ''
    state.checkInResult.status = result.status || ''
    state.checkInResult.distanceMeters = result.distanceMeters ?? null
    state.checkInResult.reason = result.failReason || result.reason || ''
    await Promise.all([fetchList(), fetchCurrentLocation()])
    if (result.success) {
      showToast(result.action === 'CHECK_OUT' ? '下班时间已补齐' : '上班打卡成功')
    } else {
      showToast(result.reason || '打卡失败')
    }
  } catch (error) {
    const message = resolveCheckInRequestErrorMessage(error)
    if (error?.response) {
      await Promise.all([fetchList(), fetchCurrentLocation()])
    }
    state.checkInResult.success = false
    state.checkInResult.allowCheckIn = false
    state.checkInResult.action = ''
    state.checkInResult.status = inferCheckInStatusFromErrorMessage(message)
    state.checkInResult.distanceMeters = null
    state.checkInResult.reason = message
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
