/*
 * Copyright (c) 2002-2014, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.recommendation.web;

import fr.paris.lutece.plugins.recommendation.service.RecommendationService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.json.ErrorJsonResponse;
import fr.paris.lutece.util.json.JsonResponse;
import fr.paris.lutece.util.json.JsonUtil;

import org.apache.mahout.cf.taste.recommender.RecommendedItem;

import java.io.IOException;

import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * Recommendation Servlet
 */
public class RecommendationServlet extends HttpServlet
{
    private static final String PARAMETER_RECOMMENDER = "recommender";
    private static final String PARAMETER_USER_ID = "id_user";
    private static final String PARAMETER_COUNT = "count";
    private static final String PROPERTY_DEFAULT_RECOMMENDER = "recommendation.default.recommender";
    private static final String PROPERTY_DEFAULT_COUNT = "recommendation.default.count";
    private static final String CONTENT_TYPE_JSON = "application/json";

    /**
     * Process GET and POST request
     * @param request The request
     * @param response The response
     * @throws ServletException if an error occurs
     * @throws IOException if an error occurs
     */
    @Override
    protected void service( HttpServletRequest request, HttpServletResponse response )
        throws ServletException, IOException
    {
        String strRecommender = request.getParameter( PARAMETER_RECOMMENDER );
        String strUserId = request.getParameter( PARAMETER_USER_ID );
        String strCount = request.getParameter( PARAMETER_COUNT );

        if ( strRecommender == null )
        {
            strRecommender = AppPropertiesService.getProperty( PROPERTY_DEFAULT_RECOMMENDER );
        }

        if ( strCount == null )
        {
            strCount = AppPropertiesService.getProperty( PROPERTY_DEFAULT_COUNT );
        }

        if ( strUserId == null )
        {
            buildErrorResponse( response, "Invalid request : Parameter '" + PARAMETER_USER_ID + "' is missing!" );

            return;
        }

        long lUserId;
        int nCount;

        try
        {
            lUserId = Long.parseLong( strUserId );
            nCount = Integer.parseInt( strCount );
        }
        catch ( NumberFormatException e )
        {
            buildErrorResponse( response, "Invalid request : invalid numeric parameter!" );

            return;
        }

        List<RecommendedItem> list = RecommendationService.instance(  )
                                                          .getRecommendations( strRecommender, lUserId, nCount );
        buildValidResponse( response, list );
    }

    /**
     * Build an error response
     * @param response The response
     * @param strMessage The message
     * @throws IOException If an error occurs
     */
    private void buildErrorResponse( HttpServletResponse response, String strMessage )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        ServletOutputStream out = response.getOutputStream(  );
        ErrorJsonResponse error = new ErrorJsonResponse( strMessage );
        out.println( JsonUtil.buildJsonResponse( error ) );
        out.close(  );
    }

    /**
     * Build a standard response
     * @param response The response
     * @param list The list of recommended items
     * @throws IOException If an error occurs
     */
    private void buildValidResponse( HttpServletResponse response, List<RecommendedItem> list )
        throws IOException
    {
        response.setContentType( CONTENT_TYPE_JSON );

        ServletOutputStream out = response.getOutputStream(  );
        JsonResponse r = new JsonResponse( list );
        out.println( JsonUtil.buildJsonResponse( r ) );
        out.close(  );
    }
}
