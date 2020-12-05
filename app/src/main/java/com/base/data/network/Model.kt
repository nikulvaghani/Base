package com.base.data.network

/**
 * Created by Nikul on 14-08-2020.
 */

enum class ResponseCode constructor(val code: Int) {
    BadRequest(400),
    Unauthenticated(401),
    Unauthorized(403),
    NotFound(404),
    RequestTimeOut(408),
    Conflict(409),
    InValidateData(422),
    Locked(423),
    ForceUpdate(426),
    ServerError(500);
}