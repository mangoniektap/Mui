# git.ps1 — Mui Git 自动化工具
# 核心逻辑：交互菜单 + 命令行分发

$Script:GitExe = $null

# ============================================================
# 输出辅助函数
# ============================================================
function Write-Info([string]$msg) {
    Write-Host "[INFO] $msg"
}

function Write-Success([string]$msg) {
    Write-Host "[SUCCESS] $msg" -ForegroundColor Green
}

function Write-Warning([string]$msg) {
    Write-Host "[WARNING] $msg" -ForegroundColor Yellow
}

function Write-Err([string]$msg) {
    Write-Host "[ERROR] $msg" -ForegroundColor Red
}

# ============================================================
# 环境检查
# ============================================================
function Test-GitInstalled {
    $git = (Get-Command git -ErrorAction SilentlyContinue)
    if (-not $git) {
        Write-Err "未检测到 Git，请先安装 Git for Windows。"
        return $false
    }
    $Script:GitExe = $git.Source
    Write-Info "Git 路径: $($Script:GitExe)"
    return $true
}

function Test-GitRepo {
    $result = & git rev-parse --git-dir 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "当前目录不是 Git 仓库。"
        return $false
    }
    return $true
}

function Test-RemoteOrigin {
    $remote = & git remote get-url origin 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "未配置远程仓库 'origin'。"
        return $false
    }
    Write-Info "远程仓库: $remote"
    return $true
}

function Test-WorkingTreeClean {
    $changes = & git status --porcelain 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git status 执行失败: $changes"
        return $false
    }
    if ([string]::IsNullOrWhiteSpace($changes)) {
        return $true
    }
    Write-Warning "工作区存在未提交的修改:"
    Write-Host $changes
    return $false
}

# ============================================================
# 状态显示
# ============================================================
function Show-GitStatus {
    # 当前分支
    $branch = & git branch --show-current 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "获取当前分支失败。"
        return
    }
    Write-Info "当前分支: $branch"

    # 远程仓库
    $remote = & git remote -v 2>&1
    if ($LASTEXITCODE -eq 0 -and $remote) {
        Write-Info "远程仓库列表:"
        Write-Host $remote
    }
    else {
        Write-Warning "未配置远程仓库。"
    }

    # 工作区状态
    Write-Info "工作区状态:"
    $status = & git status --short 2>&1
    if ($LASTEXITCODE -eq 0) {
        if ([string]::IsNullOrWhiteSpace($status)) {
            Write-Success "工作区干净，无变更。"
        }
        else {
            Write-Host $status
        }
    }
    else {
        Write-Err "git status 执行失败。"
    }
}

# ============================================================
# 推送
# ============================================================
function Invoke-Push {
    Write-Info "=== 推送 (push) ==="

    # 1-3: 环境检查
    if (-not (Test-GitInstalled)) { return }
    if (-not (Test-GitRepo)) { return }
    if (-not (Test-RemoteOrigin)) { return }

    # 4: 检查状态
    $changes = & git status --short 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git status 执行失败。"
        return
    }

    # 5: 无变更
    if ([string]::IsNullOrWhiteSpace($changes)) {
        Write-Success "未检测到变更，无需提交。"
        return
    }

    # 6: 显示文件列表
    Write-Info "待提交的文件:"
    Write-Host $changes

    # 7-8: 二次确认
    $confirm = Read-Host "`n即将执行提交并推送操作，是否继续？[y/N]"
    if ($confirm -notmatch '^(y|yes)$') {
        Write-Info "用户取消操作。"
        return
    }

    # 9: git add .
    Write-Info "执行 git add ."
    & git add .
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git add 执行失败。"
        return
    }

    # 10-11: 提交
    $msg = "$(Get-Date -Format 'yyyy-MM-dd HH:mm') 更新"
    Write-Info "提交: $msg"
    & git commit -m $msg
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git commit 执行失败。"
        return
    }

    # 12: 推送
    Write-Info "推送到远程仓库..."
    & git push
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git push 执行失败。"
        return
    }

    Write-Success "推送完成。"
}

# ============================================================
# 拉取
# ============================================================
function Invoke-Pull {
    Write-Info "=== 拉取 (pull) ==="

    # 1-3: 环境检查
    if (-not (Test-GitInstalled)) { return }
    if (-not (Test-GitRepo)) { return }
    if (-not (Test-RemoteOrigin)) { return }

    # 4-5: 工作区必须干净
    $changes = & git status --porcelain 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git status 执行失败。"
        return
    }
    if (-not [string]::IsNullOrWhiteSpace($changes)) {
        Write-Err "工作区存在未提交的修改，请先提交、暂存或放弃修改后再拉取。"
        Write-Host $changes
        return
    }

    # 6: fetch
    Write-Info "获取远程仓库信息..."
    & git fetch
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git fetch 执行失败。"
        return
    }

    # 7: pull
    Write-Info "拉取远程更新..."
    & git pull
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git pull 执行失败。"
        return
    }

    Write-Success "拉取完成。"
}

# ============================================================
# 同步（拉取变基 + 推送本地修改）
# ============================================================
function Invoke-Sync {
    Write-Info "=== 同步 (sync) ==="

    # 1-3: 环境检查
    if (-not (Test-GitInstalled)) { return }
    if (-not (Test-GitRepo)) { return }
    if (-not (Test-RemoteOrigin)) { return }

    # 4-5: 工作区必须干净
    $changes = & git status --porcelain 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git status 执行失败。"
        return
    }
    if (-not [string]::IsNullOrWhiteSpace($changes)) {
        Write-Err "工作区存在未提交的修改，请先提交、暂存或放弃修改后再同步。"
        Write-Host $changes
        return
    }

    # 6: fetch
    Write-Info "获取远程仓库信息..."
    & git fetch
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git fetch 执行失败。"
        return
    }

    # 7: pull --rebase
    Write-Info "拉取并变基..."
    & git pull --rebase
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git pull --rebase 执行失败。"
        return
    }

    # 8: 再次检查状态
    $changes = & git status --short 2>&1
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git status 执行失败。"
        return
    }

    # 9: 拉取后无变更
    if ([string]::IsNullOrWhiteSpace($changes)) {
        Write-Success "拉取后未检测到新变更。"
        return
    }

    # 10: 显示文件
    Write-Info "同步后待提交的文件:"
    Write-Host $changes

    # 11-12: 二次确认
    $confirm = Read-Host "`n即将执行提交并推送操作，是否继续？[y/N]"
    if ($confirm -notmatch '^(y|yes)$') {
        Write-Info "用户取消操作。"
        return
    }

    # 13: git add .
    Write-Info "执行 git add ."
    & git add .
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git add 执行失败。"
        return
    }

    # 14-15: 提交
    $msg = "$(Get-Date -Format 'yyyy-MM-dd HH:mm') 更新"
    Write-Info "提交: $msg"
    & git commit -m $msg
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git commit 执行失败。"
        return
    }

    # 16: 推送
    Write-Info "推送到远程仓库..."
    & git push
    if ($LASTEXITCODE -ne 0) {
        Write-Err "git push 执行失败。"
        return
    }

    Write-Success "同步完成。"
}

# ============================================================
# 交互菜单（双击启动模式）
# ============================================================
function Show-InteractiveMenu {
    while ($true) {
        Write-Host ""
        Write-Host "=============================="
        Write-Host "  Mui Git 自动化工具"
        Write-Host "=============================="
        Write-Host "  [1] 推送 (push)"
        Write-Host "  [2] 拉取 (pull)"
        Write-Host "  [3] 同步 (sync)"
        Write-Host "  [4] 状态 (status)"
        Write-Host "  [0] 退出"
        Write-Host "=============================="
        $choice = Read-Host "请选择操作（输入编号或命令名）"

        switch -Regex ($choice.Trim().ToLower()) {
            '^(1|push|推送)$' {
                Invoke-Push
            }
            '^(2|pull|拉取)$' {
                Invoke-Pull
            }
            '^(3|sync|同步)$' {
                Invoke-Sync
            }
            '^(4|status|状态)$' {
                Write-Info "=== 状态 (status) ==="
                if (Test-GitInstalled) {
                    if (Test-GitRepo) {
                        Show-GitStatus
                    }
                }
            }
            '^(0|exit|quit|q|退出)$' {
                Write-Info "再见！"
                return
            }
            default {
                Write-Warning "无效输入 '$choice'，请重新输入。"
                continue
            }
        }

        Write-Host ""
        Write-Host "按任意键返回菜单..."
        $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
    }
}

# ============================================================
# 命令行分发（CLI 模式）
# ============================================================
function Invoke-Command([string]$cmd) {
    switch -Regex ($cmd.Trim().ToLower()) {
        '^push$' {
            Invoke-Push
        }
        '^pull$' {
            Invoke-Pull
        }
        '^sync$' {
            Invoke-Sync
        }
        '^status$' {
            Write-Info "=== 状态 (status) ==="
            if (Test-GitInstalled) {
                if (Test-GitRepo) {
                    Show-GitStatus
                }
            }
        }
        default {
            Write-Err "未知命令: $cmd"
            Write-Info "可用命令: push, pull, sync, status"
            exit 1
        }
    }
}

# ============================================================
# 入口
# ============================================================
# 切换到脚本所在目录（项目根目录）
try {
    Set-Location $PSScriptRoot
}
catch {
    Write-Warning "无法切换到脚本目录: $PSScriptRoot"
}

if ($args.Count -eq 0) {
    # 交互模式（双击启动）
    try {
        Show-InteractiveMenu
    }
    catch {
        Write-Err "发生错误: $_"
        Write-Host $_.ScriptStackTrace
    }
}
else {
    # 命令行模式
    Invoke-Command $args[0]
}

# 交互模式下按任意键退出，避免窗口闪退
if ($args.Count -eq 0) {
    Write-Host ""
    Write-Host "按任意键退出..."
    $null = $Host.UI.RawUI.ReadKey('NoEcho,IncludeKeyDown')
}
