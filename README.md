
#Plugin recommendation

Powered by![](http://dev.lutece.paris.fr/plugins/plugin-recommendation/images/mahout.png)

##Introduction

This plugin provides recommendations based on machine learning library Apache Mahout.

##Configuration

This plugin can defined multiple recommenders.

Recommenders should load a large set of data to be able to provide recommendations (learning phase). Each set of data should provide those information:
 
* The user ID (as long)
* The item ID (as long)
* A preference value (as float)


Recommenders should be listed into the ` **recommendation.properties** ` file and for each one the dataset source should be defined (table and columns) as below :


```
#recommendation.properties

# List of recommenders    
recommendation.recommendersList=rec1,rec2

# recommender 1
recommendation.recommender.rec1.dataSource=portal
recommendation.recommender.rec1.preferenceTable=recommendation_default
recommendation.recommender.rec1.userIDColumn=id_user
recommendation.recommender.rec1.itemIDColumn=id_item
recommendation.recommender.rec1.preferenceColumn=preference_value
 
# recommender 2  
recommendation.recommender.rec2.dataSource=portal
...    
                
```


##Usage

The recommendations can be provided in Java by the ` **RecommendationService** ` 

```

List<RecommendedItem> list = RecommendationService.instance().getRecommendations( strRecommender, lUserId, nCount );
                
```

Or through HTTP

```

http://myserver.com/servlet/plugins/recommendation/?id_user=2&count=2&recommender=test
                
```

Default values for the recommender and the count parameters can be defined into the ` **recommendation.properties** ` file, so the url can be limited to :

```

http://myserver.com/servlet/plugins/recommendation/?id_user=2
                
```


[Maven documentation and reports](http://dev.lutece.paris.fr/plugins/plugin-recommendation/)



 *generated by [xdoc2md](https://github.com/lutece-platform/tools-maven-xdoc2md-plugin) - do not edit directly.*