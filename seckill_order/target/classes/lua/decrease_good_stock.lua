if (redis.call('exists', KEYS[2]) == 1) then
    return -9;
end
if (redis.call('exists', KEYS[1]) == 1) then
    local stock = tonumber(redis.call('get', KEYS[1]));
    local num = tonumber(ARGV[1]);
    if (stock < num) then
        return -3;
    end
    if (stock >= num) then
        redis.call('incrby', KEYS[1], 0 - num);
        return 1;
    end
    return -2
end
return -1