var engine_name = process.argv[2] == "mustache" ? "mustache" : "hogan";//hogan or mustache
var engine = require(engine_name == "hogan" ? "hogan.js" : "mustache")
  , fs    = require('fs')
  , extend= require('xtend')
  , AutoLoader = require('./classes/autoload-'+engine_name+'.js');


var path = 
{
 data : __dirname + '/../app/data',
 views : __dirname + '/../app/views',
 assets : 'assets',
 images : 'assets/images'
}

var site = JSON.parse(fs.readFileSync(path['data']+'/common/site.json' , 'utf-8'));//this site some basic site variables
site['protocol'] = 'http:'


var Sidenav_Class = require('./classes/Sidenav')
var sidenav = new Sidenav_Class()

var Page_Class = require('./classes/Page')
var Indentation = require('./classes/Indent')
var autoload = new AutoLoader(engine , path);

//iterate over all pages and generate the static html file
var page_views_folder = path["views"]+"/pages";
if(fs.existsSync(page_views_folder) && (stats = fs.statSync(page_views_folder)) && stats.isDirectory()) {
	var files = fs.readdirSync(page_views_folder)
	files.forEach(function (name) {
		var filename;//file name, which we use as the variable name
		if (! (filename = name.match(/(.+?)\.(mustache|html)$/)) ) return;
		var page_name = filename[1];
		
		generate(page_name);
	})
}


function generate(page_name) {
	var page = new Page_Class( {'engine':engine, 'path':path, 'name':page_name, 'type':'page'} );
	page.initiate();

	var layout_name = page.get_var('layout');
	var layout = new Page_Class( {'engine':engine, 'path':path, 'name':layout_name, 'type':'layout'} );
	layout.initiate();

	if(layout.get_var('sidenav_navList'))
	{
		sidenav.set_items(layout.get_var('sidenav_navList'));
		sidenav.mark_active_item(page_name);
	}


	var context = { "page":page.get_vars() , "layout":layout.get_vars(), "path" : path , "site" : site }
	context['breadcrumbs'] = sidenav.get_breadcrumbs();
	context['createLink'] = function(value) {
		return value+'.html';
	}

	autoload.set_params(page_name , layout_name);

	var rendered_output = engine_name == "hogan" ? layout.get_template().render(context) : (layout.get_template())(context)
	Indentation(rendered_output , function(result) {
		var output_file = 'output_folder/'+page_name+'.html';
		fs.writeFileSync( __dirname + '/'+output_file , result, 'utf-8' );
		console.log(output_file);
	})

}