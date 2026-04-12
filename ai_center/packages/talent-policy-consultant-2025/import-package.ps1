param(
    [string]$BackendBaseUrl = "http://127.0.0.1:8080/api",
    [string]$Username = "admin",
    [string]$Password = "admin123",
    [string]$DocumentPath = "D:\2.售前工作\4.智慧人才\99.人才推广\人才政策汇编.docx",
    [switch]$SkipPublish,
    [switch]$SkipExpertBinding
)

$ErrorActionPreference = "Stop"

function Write-Step {
    param([string]$Message)
    Write-Host ""
    Write-Host "==> $Message" -ForegroundColor Cyan
}

function Read-Package {
    $scriptDir = Split-Path -Parent $MyInvocation.ScriptName
    $packagePath = Join-Path $scriptDir "skill-package.json"
    if (-not (Test-Path $packagePath)) {
        throw "找不到材料包文件: $packagePath"
    }
    return (Get-Content $packagePath -Raw -Encoding UTF8 | ConvertFrom-Json)
}

function Invoke-JsonApi {
    param(
        [string]$Method,
        [string]$Path,
        [object]$Body,
        [string]$Token
    )

    $uri = "{0}{1}" -f $BackendBaseUrl.TrimEnd('/'), $Path
    $headers = @{}
    if ($Token) {
        $headers["Authorization"] = "Bearer $Token"
    }

    $params = @{
        Uri         = $uri
        Method      = $Method
        Headers     = $headers
        ContentType = "application/json; charset=utf-8"
    }

    if ($null -ne $Body) {
        $params["Body"] = ($Body | ConvertTo-Json -Depth 20)
    }

    $response = Invoke-RestMethod @params
    if ($null -eq $response -or $response.code -ne 0) {
        $message = if ($response) { $response.message } else { "接口未返回有效结果" }
        throw "接口调用失败 [$Path]: $message"
    }
    return $response.data
}

function Invoke-DocumentImport {
    param(
        [object]$Package,
        [long]$BaseId,
        [string]$Token
    )

    if (-not (Test-Path $DocumentPath)) {
        throw "文档不存在: $DocumentPath"
    }

    Add-Type -AssemblyName System.Net.Http

    $uri = "{0}/knowledge/document/import-docx" -f $BackendBaseUrl.TrimEnd('/')
    $client = [System.Net.Http.HttpClient]::new()
    $client.Timeout = [TimeSpan]::FromMinutes(5)
    $client.DefaultRequestHeaders.Authorization =
        [System.Net.Http.Headers.AuthenticationHeaderValue]::new("Bearer", $Token)

    $content = [System.Net.Http.MultipartFormDataContent]::new()
    try {
        $meta = $Package.knowledgeBase.importMeta

        $stringFields = @{
            baseId        = [string]$BaseId
            policyRegion  = [string]$meta.policyRegion
            policyLevel   = [string]$meta.policyLevel
            keywords      = [string]$meta.keywords
            summary       = [string]$meta.summary
            effectiveDate = ""
            expireDate    = ""
        }

        foreach ($key in $stringFields.Keys) {
            $value = $stringFields[$key]
            $content.Add([System.Net.Http.StringContent]::new($value, [System.Text.Encoding]::UTF8), $key)
        }

        $fileStream = [System.IO.File]::OpenRead($DocumentPath)
        try {
            $fileContent = [System.Net.Http.StreamContent]::new($fileStream)
            $fileContent.Headers.ContentType =
                [System.Net.Http.Headers.MediaTypeHeaderValue]::new("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
            $content.Add($fileContent, "file", [System.IO.Path]::GetFileName($DocumentPath))

            $response = $client.PostAsync($uri, $content).GetAwaiter().GetResult()
            $raw = $response.Content.ReadAsStringAsync().GetAwaiter().GetResult()
            if (-not $response.IsSuccessStatusCode) {
                throw "文档导入失败，HTTP $($response.StatusCode): $raw"
            }

            $json = $raw | ConvertFrom-Json
            if ($null -eq $json -or $json.code -ne 0) {
                $message = if ($json) { $json.message } else { "接口未返回有效 JSON" }
                throw "文档导入失败: $message"
            }
            return $json.data
        } finally {
            $fileStream.Dispose()
        }
    } finally {
        $content.Dispose()
        $client.Dispose()
    }
}

function Find-BaseByName {
    param([object[]]$BaseList, [string]$BaseName)
    return @($BaseList | Where-Object { $_.baseName -eq $BaseName }) | Select-Object -First 1
}

function Find-SkillByCode {
    param([object[]]$SkillList, [string]$SkillCode)
    return @($SkillList | Where-Object { $_.skillCode -eq $SkillCode }) | Select-Object -First 1
}

function Find-TestCaseByQuestion {
    param([object[]]$TestCaseList, [string]$QuestionText)
    return @($TestCaseList | Where-Object { $_.questionText -eq $QuestionText }) | Select-Object -First 1
}

$package = Read-Package

Write-Step "检查后端健康状态"
$health = Invoke-RestMethod -Uri ("{0}/health" -f $BackendBaseUrl.TrimEnd('/')) -Method Get -TimeoutSec 10
if ($null -eq $health -or $health.status -ne "ok") {
    throw "后端健康检查未通过，请先启动后端服务。"
}

Write-Step "管理员登录"
$loginResult = Invoke-JsonApi -Method "Post" -Path "/auth/login" -Body @{
    username = $Username
    password = $Password
}

$token = [string]$loginResult.token
if ([string]::IsNullOrWhiteSpace($token)) {
    throw "登录成功但未获取到 token。"
}

Write-Step "创建或复用知识库"
$baseList = Invoke-JsonApi -Method "Post" -Path "/knowledge/base/list" -Body @{
    keywords = $package.knowledgeBase.baseName
} -Token $token

$base = Find-BaseByName -BaseList $baseList -BaseName $package.knowledgeBase.baseName
if ($null -eq $base) {
    $baseId = Invoke-JsonApi -Method "Post" -Path "/knowledge/base/save" -Body @{
        baseName    = $package.knowledgeBase.baseName
        domainType  = $package.knowledgeBase.domainType
        description = $package.knowledgeBase.description
        remark      = $package.knowledgeBase.remark
        status      = [int]$package.knowledgeBase.status
    } -Token $token
    $base = [pscustomobject]@{
        id       = $baseId
        baseName = $package.knowledgeBase.baseName
    }
    Write-Host "已创建知识库: $($base.baseName) (#$($base.id))" -ForegroundColor Green
} else {
    Write-Host "复用已有知识库: $($base.baseName) (#$($base.id))" -ForegroundColor Yellow
}

Write-Step "设置当前知识库"
[void](Invoke-JsonApi -Method "Post" -Path "/knowledge/base/set-current" -Body @{
    id = [long]$base.id
} -Token $token)

Write-Step "导入政策文档"
$documentList = Invoke-JsonApi -Method "Post" -Path "/knowledge/document/list" -Body @{
    baseId   = [long]$base.id
    keywords = "人才政策汇编"
} -Token $token

$existingDocument = @($documentList | Where-Object {
    $_.fileName -eq [System.IO.Path]::GetFileName($DocumentPath) -or $_.docTitle -like "*人才政策汇编*"
}) | Select-Object -First 1

if ($null -eq $existingDocument) {
    $importResult = Invoke-DocumentImport -Package $package -BaseId ([long]$base.id) -Token $token
    Write-Host "文档已导入，documentId=$($importResult.documentId)，chunks=$($importResult.successChunks)/$($importResult.totalChunks)" -ForegroundColor Green
} else {
    Write-Host "检测到已有近似文档，跳过重复导入: #$($existingDocument.id) $($existingDocument.docTitle)" -ForegroundColor Yellow
}

Write-Step "创建或复用 Skill"
$skillList = Invoke-JsonApi -Method "Post" -Path "/skill/list" -Body @{
    keywords = $package.skill.skillCode
} -Token $token

$skill = Find-SkillByCode -SkillList $skillList -SkillCode $package.skill.skillCode
if ($null -eq $skill) {
    $skillId = Invoke-JsonApi -Method "Post" -Path "/skill/save" -Body @{
        skillCode   = $package.skill.skillCode
        skillName   = $package.skill.skillName
        domainType  = $package.skill.domainType
        skillType   = $package.skill.skillType
        description = $package.skill.description
        status      = 1
    } -Token $token
    $skill = [pscustomobject]@{
        id        = $skillId
        skillCode = $package.skill.skillCode
        skillName = $package.skill.skillName
    }
    Write-Host "已创建 Skill: $($skill.skillName) (#$($skill.id))" -ForegroundColor Green
} else {
    Write-Host "复用已有 Skill: $($skill.skillName) (#$($skill.id))" -ForegroundColor Yellow
}

Write-Step "准备 Skill 版本"
$providerList = Invoke-JsonApi -Method "Post" -Path "/ai/provider/list" -Body @{ status = 1 } -Token $token
$provider = @($providerList | Select-Object -First 1)
$providerId = if ($provider) { [long]$provider[0].id } else { $null }
$modelCode = if ($provider -and $provider[0].defaultModel) { [string]$provider[0].defaultModel } else { "" }

$publishedVersion = $null
try {
    $publishedVersion = Invoke-JsonApi -Method "Get" -Path "/skill/$($skill.id)/published-version" -Body $null -Token $token
} catch {
    $publishedVersion = $null
}

$versionBody = @{
    skillId        = [long]$skill.id
    versionNo      = $package.version.versionNo
    providerConfigId = $providerId
    modelCode      = $modelCode
    systemPrompt   = $package.version.systemPrompt
    taskPrompt     = $package.version.taskPrompt
    outputTemplate = $package.version.outputTemplate
    forbiddenRules = $package.version.forbiddenRules
    citationRules  = $package.version.citationRules
}

if ($publishedVersion -and $publishedVersion.versionNo -eq $package.version.versionNo) {
    $versionBody["id"] = [long]$publishedVersion.id
}

$skillVersionId = Invoke-JsonApi -Method "Post" -Path "/skill/version/save" -Body $versionBody -Token $token
Write-Host "Skill 版本已保存: #$skillVersionId" -ForegroundColor Green

Write-Step "绑定知识库"
[void](Invoke-JsonApi -Method "Post" -Path "/skill/binding/save" -Body @{
    skillId        = [long]$skill.id
    skillVersionId = [long]$skillVersionId
    baseId         = [long]$base.id
} -Token $token)

Write-Step "同步验证题"
$existingCases = Invoke-JsonApi -Method "Post" -Path "/skill/test-case/list" -Body @{
    skillVersionId = [long]$skillVersionId
} -Token $token

foreach ($testCase in $package.testCases) {
    $existingCase = Find-TestCaseByQuestion -TestCaseList $existingCases -QuestionText $testCase.questionText
    $body = @{
        skillId        = [long]$skill.id
        skillVersionId = [long]$skillVersionId
        caseType       = $testCase.caseType
        questionText   = $testCase.questionText
        expectedPoints = $testCase.expectedPoints
        expectedFormat = $testCase.expectedFormat
        standardAnswer = $testCase.standardAnswer
        status         = 1
    }
    if ($existingCase) {
        $body["id"] = [long]$existingCase.id
    }
    [void](Invoke-JsonApi -Method "Post" -Path "/skill/test-case/save" -Body $body -Token $token)
}
Write-Host "验证题已同步: $($package.testCases.Count) 条" -ForegroundColor Green

if (-not $SkipPublish) {
    Write-Step "发布 Skill 版本"
    [void](Invoke-JsonApi -Method "Post" -Path "/skill/version/publish" -Body @{
        skillVersionId = [long]$skillVersionId
    } -Token $token)
    Write-Host "Skill 已发布" -ForegroundColor Green
}

if (-not $SkipExpertBinding) {
    Write-Step "绑定默认专家身份"
    [void](Invoke-JsonApi -Method "Post" -Path "/expert/save" -Body @{
        userId         = [long]$loginResult.userId
        skillId        = [long]$skill.id
        skillVersionId = [long]$skillVersionId
        expertLevel    = [string]$package.expertBinding.expertLevel
        status         = [int]$package.expertBinding.status
    } -Token $token)
    Write-Host "已为用户 #$($loginResult.userId) 绑定专家身份" -ForegroundColor Green
}

Write-Step "导入完成"
Write-Host "知识库 ID: $($base.id)" -ForegroundColor Green
Write-Host "Skill ID: $($skill.id)" -ForegroundColor Green
Write-Host "SkillVersion ID: $skillVersionId" -ForegroundColor Green
if ($providerId) {
    Write-Host "已关联 AI Provider: $providerId ($modelCode)" -ForegroundColor Green
} else {
    Write-Host "当前系统未发现启用中的 AI Provider；知识库和 Skill 已就绪，但在线问答仍需后续补齐 AI 接入。" -ForegroundColor Yellow
}
