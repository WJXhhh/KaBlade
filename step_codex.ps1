#Requires -Version 5.1
<#
.SYNOPSIS
    Start Codex CLI with StepFun as a temporary provider.

.USAGE
    .\codex-stepfun.ps1
    .\codex-stepfun.ps1 -Yolo
    .\codex-stepfun.ps1 -Model step-3.7-flash
    .\codex-stepfun.ps1 exec "检查这个项目的问题"
    #>
param(
    [string]$Model = "step-3.7-flash",

    # 类似 Claude Code 的 --dangerously-skip-permissions。
    # 注意：这会绕过 Codex 的审批和沙箱，只建议在可信项目里用。
    [switch]$Yolo,

    # 把剩余参数原样传给 codex，例如：exec "review this repo"
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$CodexArgs
)

$ErrorActionPreference = "Stop"

# ============================================================
# 你只需要改这里
# ============================================================
$StepFunApiKey = "3iwIfRZKLmGN09d3LQRwzCIxTNqG4ERavlkaHPD1BjrbwwlBWW21nIswIcJPAROq8"

# 可选：如果你希望 Codex 执行 gh / git 相关命令时能用 GitHub token，可以填这里。
# 不需要就留空。
$GitHubToken = ""

# ============================================================
# 一般不用改下面
# ============================================================
$CodexHome = Join-Path $env:USERPROFILE ".codex"
$ConfigPath = Join-Path $CodexHome "stepfun.config.toml"

# 保存旧环境，脚本退出时恢复，避免污染当前终端
$OldCodexHome = $env:CODEX_HOME
$HadStepKey = Test-Path Env:\STEP_API_KEY
$OldStepKey = $env:STEP_API_KEY

$HadGhToken = Test-Path Env:\GH_TOKEN
$OldGhToken = $env:GH_TOKEN
$HadGithubToken = Test-Path Env:\GITHUB_TOKEN
$OldGithubToken = $env:GITHUB_TOKEN
$HadGithubPat = Test-Path Env:\GITHUB_PERSONAL_ACCESS_TOKEN
$OldGithubPat = $env:GITHUB_PERSONAL_ACCESS_TOKEN

$ExitCode = 0

try {
    if ([string]::IsNullOrWhiteSpace($StepFunApiKey) -or $StepFunApiKey -eq "在这里填你的阶跃星辰APIKey") {
        throw "请先在脚本里把 `$StepFunApiKey 改成你的阶跃星辰 API Key。"
    }

    New-Item -ItemType Directory -Force -Path $CodexHome | Out-Null

    # 只对本次启动有效
    $env:CODEX_HOME = $CodexHome
    $env:STEP_API_KEY = $StepFunApiKey

    if (-not [string]::IsNullOrWhiteSpace($GitHubToken)) {
        # GitHub CLI 常用 GH_TOKEN / GITHUB_TOKEN；保留 GITHUB_PERSONAL_ACCESS_TOKEN 兼容你原来的习惯。
        $env:GH_TOKEN = $GitHubToken
        $env:GITHUB_TOKEN = $GitHubToken
        $env:GITHUB_PERSONAL_ACCESS_TOKEN = $GitHubToken
    }

    $Toml = @"
model = "step-3.7-flash"
model_provider = "stepfun"

model_reasoning_effort = "high"
plan_mode_reasoning_effort = "high"

# 给 Codex 一个明确的上下文窗口，避免 fallback 太保守
model_context_window = 256000

# 建议比上下文窗口低一点，留出输出/工具调用/系统提示余量
model_auto_compact_token_limit = 220000

approval_policy = "on-request"
sandbox_mode = "workspace-write"

[model_providers.stepfun]
name = "StepFun"
base_url = "https://api.stepfun.com/v1"
env_key = "STEP_API_KEY"
requires_openai_auth = false
wire_api = "responses"

[features]
memories = true

[memories]
generate_memories = true
use_memories = true
disable_on_external_context = false
"@

    # 写 UTF-8 无 BOM，避免 TOML 解析器遇到奇怪编码问题
    $Utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    if (-not (Test-Path -LiteralPath $ConfigPath)) {
    # 不存在才创建默认 stepfun.config.toml
    [System.IO.File]::WriteAllText($ConfigPath, $Toml, $Utf8NoBom)
    Write-Host "Created StepFun profile config: $ConfigPath"
}
else {
    # 已存在就不覆盖
    Write-Host "Using existing StepFun profile config: $ConfigPath"
}

    $RunArgs = @("--profile", "stepfun", "--cd", $PSScriptRoot)

    if ($Yolo) {
        $RunArgs += "--dangerously-bypass-approvals-and-sandbox"
    }

    if ($CodexArgs -and $CodexArgs.Count -gt 0) {
        $RunArgs += $CodexArgs
    }

    Write-Host "Using CODEX_HOME: $CodexHome"
    Write-Host "Using StepFun model: $Model"
    Write-Host "Starting: codex $($RunArgs -join ' ')"

    & codex @RunArgs

    if ($null -ne $LASTEXITCODE) {
        $ExitCode = $LASTEXITCODE
    }
}
catch {
    Write-Host "启动失败：$($_.Exception.Message)" -ForegroundColor Red
    $ExitCode = 1
}
finally {
    if ($null -eq $OldCodexHome) {
        Remove-Item Env:\CODEX_HOME -ErrorAction SilentlyContinue
    } else {
        $env:CODEX_HOME = $OldCodexHome
    }

    if ($HadStepKey) {
        $env:STEP_API_KEY = $OldStepKey
    } else {
        Remove-Item Env:\STEP_API_KEY -ErrorAction SilentlyContinue
    }

    if ($HadGhToken) {
        $env:GH_TOKEN = $OldGhToken
    } else {
        Remove-Item Env:\GH_TOKEN -ErrorAction SilentlyContinue
    }

    if ($HadGithubToken) {
        $env:GITHUB_TOKEN = $OldGithubToken
    } else {
        Remove-Item Env:\GITHUB_TOKEN -ErrorAction SilentlyContinue
    }

    if ($HadGithubPat) {
        $env:GITHUB_PERSONAL_ACCESS_TOKEN = $OldGithubPat
    } else {
        Remove-Item Env:\GITHUB_PERSONAL_ACCESS_TOKEN -ErrorAction SilentlyContinue
    }
}

exit $ExitCode