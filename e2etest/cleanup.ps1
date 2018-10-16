Param(
    [string] $pid
)

Stop-Process -Id $pid -Erroraction Ignore