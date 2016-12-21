/*
 * Copyright (c) 2002-2017, Mairie de Paris
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
package fr.paris.lutece.plugins.recommendation.service;

import fr.paris.lutece.portal.service.database.AppConnectionService;
import fr.paris.lutece.portal.service.util.AppLogService;
import fr.paris.lutece.portal.service.util.AppPathService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.pool.PoolManager;
import java.io.File;
import java.io.IOException;

import org.apache.mahout.cf.taste.common.TasteException;
import org.apache.mahout.cf.taste.impl.model.jdbc.MySQLJDBCDataModel;
import org.apache.mahout.cf.taste.impl.neighborhood.ThresholdUserNeighborhood;
import org.apache.mahout.cf.taste.impl.recommender.GenericUserBasedRecommender;
import org.apache.mahout.cf.taste.impl.similarity.PearsonCorrelationSimilarity;
import org.apache.mahout.cf.taste.model.DataModel;
import org.apache.mahout.cf.taste.neighborhood.UserNeighborhood;
import org.apache.mahout.cf.taste.recommender.RecommendedItem;
import org.apache.mahout.cf.taste.recommender.UserBasedRecommender;
import org.apache.mahout.cf.taste.similarity.UserSimilarity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;
import org.apache.mahout.cf.taste.common.NoSuchUserException;
import org.apache.mahout.cf.taste.impl.model.file.FileDataModel;


/**
 * RecommendationService
 */
public final class RecommendationService
{
    private static final String PROPERTY_LIST = "recommendation.recommendersList";
    private static final String PREFIX = "recommendation.recommender.";
    private static final String PROPERTY_DATA_FILE = ".dataFile";
    private static final String PROPERTY_DATASOURCE = ".dataSource";
    private static final String PROPERTY_PREF_TABLE = ".preferenceTable";
    private static final String PROPERTY_USER_ID_COL = ".userIDColumn";
    private static final String PROPERTY_ITEM_ID_COL = ".itemIDColumn";
    private static final String PROPERTY_PREF_COL = ".preferenceColumn";
    private static final String PROPERTY_THRESHOLD = ".threshold";
    private static final String DEFAULT_THRESHOLD = "0.1";
    private static final List<RecommendedItem> LIST_NO_RECOMMENDATION = new ArrayList<RecommendedItem>(  );
    private static Map<String, UserBasedRecommender> _mapRecommenders;
    private static RecommendationService _singleton;

    /** Private constructor */
    private RecommendationService(  )
    {
    }

    /**
     * Provides the unique instance
     * @return the unique instance
     */
    public static synchronized RecommendationService instance(  )
    {
        if ( _singleton == null )
        {
            _singleton = new RecommendationService(  );
            init(  );
        }

        return _singleton;
    }

    /**
     * Initialize the service
     */
    private static void init(  )
    {
        _mapRecommenders = new HashMap<String, UserBasedRecommender>(  );

        String strList = AppPropertiesService.getProperty( PROPERTY_LIST );
        String[] recommenders = strList.split( "," );

        for ( String strRecommender : recommenders )
        {
            UserBasedRecommender recommender = initRecommender( strRecommender.trim(  ) );
            _mapRecommenders.put( strRecommender.trim(), recommender );
            AppLogService.info( "New Mahout Recommender registered '" + strRecommender + "'" );
        }
    }

    /**
     * Provides a list of recommended items for a given user based on a recommender
     * @param strRecommender The recommender name
     * @param lUserID The User's ID
     * @param nCount The number of recommendation whished
     * @return The list of recommended items
     * @throws NoSuchUserException if no user was found
     * @throws NoRecommenderException if no recommender was found
     */
    public List<RecommendedItem> getRecommendations( String strRecommender, long lUserID, int nCount ) throws NoSuchUserException, NoRecommenderException
    {
        UserBasedRecommender recommender = _mapRecommenders.get( strRecommender );

        if ( recommender != null )
        {
            try
            {
                return recommender.recommend( lUserID, nCount );
            }
            catch ( TasteException ex )
            {
                if( ex instanceof NoSuchUserException )
                {
                    throw (NoSuchUserException) ex;
                }
                AppLogService.error( "Error  getting recommendation : " + ex.getMessage(  ), ex );
            }
        }
        else
        {
            throw new NoRecommenderException( "Recommender not found" );
        }

        return LIST_NO_RECOMMENDATION;
    }

    /**
     * Initialize a recommender
     * @param strName The recommender name
     * @return The recommender
     */
    private static UserBasedRecommender initRecommender( String strName ) 
    {
        try
        {
            AppLogService.info( "Initialize Mahout DataModel for Recommender '" + strName + "'" );

            String strKeyPrefix = PREFIX + strName;
            
            String strFile = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_DATA_FILE );
            DataModel model;
            
            if( strFile != null )
            {
                AppLogService.info( "- Loading data from file = " + strFile );
                String strPath = AppPathService.getAbsolutePathFromRelativePath( strFile );
                model = new FileDataModel( new File( strPath ) );
            }
            else
            {
                String strDataSource = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_DATASOURCE );
                AppLogService.info( "- DataSource = " + strDataSource );

                String strPrefTable = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_PREF_TABLE );
                AppLogService.info( "- Table = " + strPrefTable );

                String strUserIdColumn = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_USER_ID_COL );
                AppLogService.info( "- User ID Column = " + strUserIdColumn );

                String strItemIdColumn = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_ITEM_ID_COL );
                AppLogService.info( "- Item ID Column = " + strItemIdColumn );

                String strPrefColumn = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_PREF_COL );
                AppLogService.info( "- Pref Column = " + strPrefColumn );

                PoolManager pm = AppConnectionService.getPoolManager(  );
                DataSource dataSource = pm.getDataSource( strDataSource );


                model = new MySQLJDBCDataModel( dataSource, strPrefTable, strUserIdColumn, strItemIdColumn,
                        strPrefColumn, null );

            }
            UserSimilarity similarity = new PearsonCorrelationSimilarity( model );
            String strThreshold = AppPropertiesService.getProperty( strKeyPrefix + PROPERTY_THRESHOLD , DEFAULT_THRESHOLD );
            AppLogService.info( "- Threshold for recommender '" + strName + "' = " + strThreshold );
            double threshold = Double.valueOf( strThreshold );
            UserNeighborhood neighborhood = new ThresholdUserNeighborhood( threshold , similarity, model );
            

            return new GenericUserBasedRecommender( model, neighborhood, similarity );
        }
        catch ( TasteException | IOException ex )
        {
            AppLogService.error( "Error loading recommender : " + ex.getMessage(  ), ex );
        }

        return null;
    }
}
