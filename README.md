
# Quota for java

This is an overcomplicated library to manage your resources' quota. 

It's overcomplicated because I needed it to be:

- multi-tenant: manage multiple users 
- extendable: Comes with a `QuantityOverTime` default type of quota, but it can be configured to use quotas that are based 
on anything. Just need to extend: `public interface QuotaManager<T>`
- durable (persisted in a database): quota state needs to outlive the process where it's created

If you don't need those things, I'd use something else as this is going to be overkill.

A possible use case: you have a paid plan with predefined resource limits, this library will help you enforce those limits


### Example 
In this example, we have a website that can compress images. 
We want to make sure the user can only use the `compress-image` feature, no more 10 times per day.


1) Every resource you want to limit the use of, can be associated with a type of quota. In this example,
`compress-image` will be associated with a quota of type `QuantityOverTime` with a default state of `10 per day`

        QuantityOverTimeState defaultState = new QuantityOverTimeState(QuantityOverTimeLimit.limitOf(1, Duration.ofSeconds(1)), 10, Instant.EPOCH);
        Quota quota = new Quota("compress-image", QuantityOverTimeQuotaManager.class.getName(), defaultState);
        quotaPersistence.save(quota);
This would be done in a back office function, where an admin can configure and add new quota types. 

2) You instantiate `QuotaService`


     QuotaService quotaService = new QuotaService(quotaPersistence, quotaStatePersistence)

`quotaPersistence` is a repository that allows the library to fetch the type of quota associated with a specific
resource. For example, in this case, the `compress-image` quota type will be fetched to understand how to manage this resource

3) You try to acquire a resource


    quotaService.tryAcquire("user-id", `compress-image`, 1);

    




