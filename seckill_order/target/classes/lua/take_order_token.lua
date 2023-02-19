---
--- Generated by EmmyLua(https://github.com/EmmyLua)
--- Created by Ezreal.
--- DateTime: 2023/1/12 22:48
---

if (redis.call('exists', KEYS[1]) == 1) then
    local availableTokensCount = tonumber(redis.call('get', KEYS[1]))
    if availableTokensCount == 0 then
        return -1
    end
    if availableTokensCount > 0 then
        redis.call('incrby', KEYS[1], -1)
        return 1
    end
end

return -100